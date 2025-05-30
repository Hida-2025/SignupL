package hida.example.signupl;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class SelectRoleActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);

        Button studentButton = findViewById(R.id.studentButton);
        Button employerButton = findViewById(R.id.employerButton);

        studentButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        });

        employerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpEmployer.class);
            startActivity(intent);
        });
    }
}
