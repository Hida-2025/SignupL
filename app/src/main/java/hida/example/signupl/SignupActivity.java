package hida.example.signupl;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.Calendar;

public class SignupActivity extends AppCompatActivity {

    private EditText firstName, lastName, birthDate, phoneNumber, email;
    private RadioGroup radioGroupGender;
    private Button btnSignup;
    private ImageButton btnGoogleSignin, btnFacebookSignin;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    private ActivityResultLauncher<Intent> googleLauncher;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager callbackManager; // Facebook

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Views
        firstName = findViewById(R.id.edit_first_name);
        lastName = findViewById(R.id.edit_last_name);
        birthDate = findViewById(R.id.edit_birth_date);
        phoneNumber = findViewById(R.id.edit_phone_number);
        email = findViewById(R.id.edit_email);
        radioGroupGender = findViewById(R.id.radio_group_gender);
        btnSignup = findViewById(R.id.btn_login);
        btnGoogleSignin = findViewById(R.id.btn_google_signin);
        btnFacebookSignin = findViewById(R.id.btn_facebook_signin);

        // Date picker
        birthDate.setOnClickListener(v -> showDatePicker());

        btnSignup.setOnClickListener(v -> signupWithEmail());

        setupGoogleSignIn();
        setupFacebookSignIn();
    }

    private void signupWithEmail() {
        String fname = firstName.getText().toString();
        String lname = lastName.getText().toString();
        String dob = birthDate.getText().toString();
        String phone = phoneNumber.getText().toString();
        String mail = email.getText().toString();

        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedGenderId);
        String gender = selectedGender != null ? selectedGender.getText().toString() : "";

        if (fname.isEmpty() || lname.isEmpty() || dob.isEmpty() || phone.isEmpty() || mail.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(mail, "defaultPassword123") // Change or remove if not used
                .addOnSuccessListener(authResult -> {
                    String uid = mAuth.getCurrentUser().getUid();
                    UserHelperClass user = new UserHelperClass(fname, lname, dob, gender, phone, mail);
                    usersRef.child(uid).setValue(user);
                    Toast.makeText(this, "Signed up successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, day) -> birthDate.setText(day + "/" + (month + 1) + "/" + year),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    // ---------------------- GOOGLE ----------------------
    private void setupGoogleSignIn() {
        googleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account);
                } catch (ApiException e) {
                    Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleSignin.setOnClickListener(v -> googleLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    String uid = user.getUid();
                    UserHelperClass userData = new UserHelperClass(user.getDisplayName(), "", "", "", "", user.getEmail());
                    usersRef.child(uid).setValue(userData);
                    Toast.makeText(this, "Signed in with Google", Toast.LENGTH_SHORT).show();
                });
    }

    // ---------------------- FACEBOOK ----------------------
    private void setupFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create();

        btnFacebookSignin.setOnClickListener(v -> LoginManager.getInstance().logInWithReadPermissions(SignupActivity.this, Arrays.asList("email", "public_profile")));

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(SignupActivity.this, "Facebook login cancelled", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(SignupActivity.this, "Facebook login error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    String uid = user.getUid();
                    UserHelperClass userData = new UserHelperClass(user.getDisplayName(), "", "", "", "", user.getEmail());
                    usersRef.child(uid).setValue(userData);
                    Toast.makeText(this, "Signed in with Facebook", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}



