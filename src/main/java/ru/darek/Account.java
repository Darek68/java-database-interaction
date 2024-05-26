package ru.darek;

@RepositoryTable(title = "accounts")
public class Account {
    @RepositoryIdField
    @RepositoryField
    private Long id;

    @RepositoryField
    private Long amount;

    @RepositoryField // TODO (name = "tp");
    private String acctype; // acctType

    @RepositoryField
    private String status;

    public Account(Long amount, String accountType, String status) {
        this.amount = amount;
        this.acctype = accountType;
        this.status = status;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", amount=" + amount +
                ", accountType='" + acctype + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
