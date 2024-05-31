package ru.darek;

@RepositoryTable(title = "accounts")
public class Account {
    @RepositoryIdField
    @RepositoryField
    private Long id;

    @RepositoryField
    private Long amount;

    @RepositoryField // TODO (name = "tp");
    @RepositoryFieldName(title = "account_type")
    private String acctType;

    @RepositoryField
    private String status;

    public Account() {
    }
    public Account(Long amount, String accountType, String status) {
        this.amount = amount;
        this.acctType = accountType;
        this.status = status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
    public void setAcctType(String acctType) { this.acctType = acctType;}

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", amount=" + amount +
              //  ", accountType='" + acctype + '\'' +
                ", accountType='" + acctType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
