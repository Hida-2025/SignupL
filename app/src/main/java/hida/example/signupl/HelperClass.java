package hida.example.signupl;

public class HelperClass {
    String name, mail, password, gender, provider;

    public HelperClass(String name, String mail, String password, String gender, String provider) {
        this.name = name;
        this.mail = mail;
        this.password = password;
        this.gender = gender;
        this.provider = provider;
    }

    public HelperClass() {}

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
