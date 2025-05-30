package hida.example.signupl;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class SignUpEmployer extends AppCompatActivity {

    EditText editName, editDate, editPhone, editEmail, editAddress;
    Button btnSignup;
    ImageButton btnRetour;

    FirebaseDatabase database;
    DatabaseReference companyRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signupemp); // Change le nom selon ton XML

        editName = findViewById(R.id.edit_name_company);
        editDate = findViewById(R.id.edit_birth_date);
        editPhone = findViewById(R.id.edit_phone_number);
        editEmail = findViewById(R.id.edit_email);
        editAddress = findViewById(R.id.edit_address);
        btnSignup = findViewById(R.id.btn_login);
        btnRetour = findViewById(R.id.btnRetour);

        // Firebase
        database = FirebaseDatabase.getInstance();
        companyRef = database.getReference("companies");

        // Sélecteur de date
        editDate.setOnClickListener(view -> showDatePicker());

        // Bouton retour
        btnRetour.setOnClickListener(v -> finish());

        // Enregistrement dans Firebase
        btnSignup.setOnClickListener(view -> registerCompany());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String date = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
            editDate.setText(date);
        }, year, month, day).show();
    }

    private void registerCompany() {
        String name = editName.getText().toString().trim();
        String date = editDate.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        if (name.isEmpty() || date.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Company company = new Company(name, date, phone, email, address);

        // Clé unique auto-générée
        companyRef.push().setValue(company)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Company registered", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to register: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
