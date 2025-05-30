package hida.example.signupl;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.*;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import org.json.JSONException;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private EditText loginName, loginPassword;
    private Button loginButton;
    private ImageButton googleLogin, facebookLogin;
    private TextView forgotPassword, signupRedirect;
    private ProgressBar loginProgress;

    private FirebaseAuth firebaseAuth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private CallbackManager callbackManager;

    private final ActivityResultLauncher<IntentSenderRequest> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                        String idToken = credential.getGoogleIdToken();
                        if (idToken != null) {
                            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                            firebaseAuth.signInWithCredential(firebaseCredential)
                                    .addOnCompleteListener(this, task -> {
                                        if (task.isSuccessful()) {
                                            showToast("Connexion Google réussie");
                                            goToMainActivity();
                                        } else {
                                            showToast("Échec de la connexion Google");
                                        }
                                    });
                        }
                    } catch (Exception e) {
                        showToast("Erreur Google Sign-In: " + e.getMessage());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        firebaseAuth = FirebaseAuth.getInstance();
        oneTapClient = Identity.getSignInClient(this);
        callbackManager = CallbackManager.Factory.create();

        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                .build();

        loginName = findViewById(R.id.loginName);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        googleLogin = findViewById(R.id.googleLogin);
        facebookLogin = findViewById(R.id.facebookLogin);
        forgotPassword = findViewById(R.id.forgotPassword);
        signupRedirect = findViewById(R.id.signupRedirect);
        loginProgress = findViewById(R.id.loginProgress);

        loginButton.setOnClickListener(v -> handleClassicLogin());
        googleLogin.setOnClickListener(v -> handleGoogleLogin());
        facebookLogin.setOnClickListener(v -> handleFacebookLogin());
        forgotPassword.setOnClickListener(v -> handleForgotPassword());

        // ✅ Redirection vers SelectRoleActivity au lieu de SignupActivity
        signupRedirect.setOnClickListener(v -> startActivity(new Intent(this, SelectRoleActivity.class)));
    }

    private void handleClassicLogin() {
        String name = loginName.getText().toString().trim().toLowerCase();
        String password = loginPassword.getText().toString().trim();

        if (name.isEmpty() || password.isEmpty()) {
            showToast("Veuillez remplir tous les champs");
            return;
        }

        loginProgress.setVisibility(View.VISIBLE);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        reference.child(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loginProgress.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    String dbPassword = snapshot.child("password").getValue(String.class);
                    if (dbPassword != null && dbPassword.equals(password)) {
                        showToast("Connexion réussie");
                        goToMainActivity();
                    } else {
                        showToast("Mot de passe incorrect");
                    }
                } else {
                    showToast("Utilisateur introuvable");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loginProgress.setVisibility(View.GONE);
                showToast("Erreur : " + error.getMessage());
            }
        });
    }

    private void handleGoogleLogin() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build();
                    googleSignInLauncher.launch(request);
                })
                .addOnFailureListener(e -> showToast("Erreur Google Sign-In: " + e.getMessage()));
    }

    private void handleFacebookLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                showToast("Connexion Facebook annulée");
            }

            @Override
            public void onError(FacebookException error) {
                showToast("Erreur Facebook: " + error.getMessage());
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        showToast("Connexion Facebook réussie");
                        goToMainActivity();
                    } else {
                        showToast("Échec de l'authentification Facebook");
                    }
                });
    }

    private void handleForgotPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Réinitialiser le mot de passe");

        final EditText input = new EditText(this);
        input.setHint("Entrez votre email");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Envoyer", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                firebaseAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                showToast("Email de réinitialisation envoyé");
                            } else {
                                showToast("Échec : " + task.getException().getMessage());
                            }
                        });
            } else {
                showToast("Email vide");
            }
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class); // Remplacez MainActivity par l’activité principale réelle
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}






