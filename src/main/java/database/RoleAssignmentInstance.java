package database;

public class RoleAssignmentInstance extends ReactionEvent {

    private String roleId;

    public String getRoleId() {
        return roleId;
    }

    public String getRoleMention(){
        return "<@&" + roleId + ">";
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public RoleAssignmentInstance(String roleId) {
        this.roleId = roleId;
    }
    public RoleAssignmentInstance(String roleId, String emojiName, String emojiId) {
        this.roleId = roleId;
        this.emojiId = emojiId;
        this.emojiName = emojiName;
    }
}
