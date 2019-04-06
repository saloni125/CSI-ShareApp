package DbSchema;

public class UserStructure {
    String name, email, photoUrl;

    public UserStructure() {
    }

    public UserStructure(String name, String email, String photoUrl) {
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}

