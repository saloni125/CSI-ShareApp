package DbSchema;

public class SharedListStructure {
    String listName, listKey;

    public SharedListStructure() {
    }

    public SharedListStructure(String listName, String listKey) {
        this.listName = listName;
        this.listKey = listKey;
    }

    public String getListName() {
        return listName;
    }

    public String getListKey() {
        return listKey;
    }
}

