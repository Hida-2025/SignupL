package hida.example.signupl;
public class Company {
    public String name, foundedDate, phone, email, address;

    public Company() {
        // Obligatoire pour Firebase
    }

    public Company(String name, String foundedDate, String phone, String email, String address) {
        this.name = name;
        this.foundedDate = foundedDate;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }
}
