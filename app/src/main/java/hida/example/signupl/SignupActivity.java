package hida.example.signupl;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.*;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;

public class SignupActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword;
    private RadioGroup radioGroupGender;
    private Button signupButton;
    private ImageButton googleButton, facebookButton;
    private TextView loginRedirectText;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private CallbackManager callbackManager;

    private ActivityResultLauncher<IntentSenderRequest> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.signup);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        // UI
        editTextName = findViewById(R.id.yourEditName);
        editTextEmail = findViewById(R.id.yourEditTextName);
        editTextPassword = findViewById(R.id.yourpasw);
        radioGroupGender = findViewById(R.id.radio_group_gender);
        signupButton = findViewById(R.id.signup);
        googleButton = findViewById(R.id.google_image_button);
        facebookButton = findViewById(R.id.facebook_image_button); // Ajout FB
        loginRedirectText = findViewById(R.id.login_link);

        signupButton.setOnClickListener(v -> registerWithEmail());
        googleButton.setOnClickListener(v -> signInWithGoogle());
        facebookButton.setOnClickListener(v -> signInWithFacebook());

        loginRedirectText.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Google One Tap
        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .setAutoSelectEnabled(true)
                .build();

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleGoogleSignInResult(result.getData());
                    }
                });

        // Facebook
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(SignupActivity.this, "Facebook Sign-In canceled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(SignupActivity.this, "Facebook Sign-In error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerWithEmail() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        int selectedId = radioGroupGender.getCheckedRadioButtonId();
        RadioButton selectedGenderButton = findViewById(selectedId);
        String gender = (selectedGenderButton != null) ? selectedGenderButton.getText().toString() : "";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        User user = new User(name, email, gender);
                        mDatabase.child(uid).setValue(user)
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Sign up successful", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Sign up failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    IntentSenderRequest intentSenderRequest =
                            new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build();
                    googleSignInLauncher.launch(intentSenderRequest);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleGoogleSignInResult(Intent data) {
        try {
            SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
            String idToken = credential.getGoogleIdToken();

            if (idToken != null) {
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                mAuth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                if (firebaseUser != null) {
                                    String uid = firebaseUser.getUid();
                                    String name = firebaseUser.getDisplayName();
                                    String email = firebaseUser.getEmail();

                                    User user = new User(name, email, "Google");
                                    mDatabase.child(uid).setValue(user)
                                            .addOnSuccessListener(aVoid ->
                                                    Toast.makeText(this, "Google Sign-In successful", Toast.LENGTH_SHORT).show());
                                }
                            } else {
                                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } catch (ApiException e) {
            e.printStackTrace();
            Toast.makeText(this, "Google Sign-In error", Toast.LENGTH_SHORT).show();
        }
    }

    private void signInWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            String name = firebaseUser.getDisplayName();
                            String email = firebaseUser.getEmail();

                            User user = new User(name, email, "Facebook");
                            mDatabase.child(uid).setValue(user)
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(this, "Facebook Sign-In successful", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(this, "Facebook authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public static class User {
        public String name, email, gender;

        public User() {}

        public User(String name, String email, String gender) {
            this.name = name;
            this.email = email;
            this.gender = gender;
        }
    }
}







