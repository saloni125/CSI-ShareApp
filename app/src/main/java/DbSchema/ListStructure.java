package DbSchema;

public class ListStructure {
    String listName, owner;
    long timestampCreated, timestampLastUpdated;
//    Map<String, ListMemberStructure> members;
//    Map<String, ExpenseStructure> expenses;

    public ListStructure() {

    }

    public ListStructure(String listName, String owner, long timestampCreated, long timestampLastUpdated) {
        this.listName = listName;
        this.owner = owner;
        this.timestampCreated = timestampCreated;
        this.timestampLastUpdated = timestampLastUpdated;
    }
    public String getListName() {
        return listName;
    }

    public String getOwner() {
        return owner;
    }

    public long getTimestampCreated() {
        return timestampCreated;
    }

    public long getTimestampLastUpdated() {
        return timestampLastUpdated;
    }

}
