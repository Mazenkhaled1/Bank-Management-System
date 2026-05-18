package repository;

import models.Account;
import utils.FileManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccountRepository implements Repository<Account, String> {

    private static final String FILE_NAME = "accounts.dat";
    private final Map<String, Account> store;
    private final FileManager fileManager;

    @SuppressWarnings("unchecked")
    public AccountRepository(FileManager fileManager) {
        this.fileManager = fileManager;
        Map<String, Account> loaded = (Map<String, Account>) fileManager.load(FILE_NAME);
        this.store = (loaded != null) ? loaded : new HashMap<>();
    }

    @Override
    public void save(Account account) {
        store.put(account.getAccountNumber(), account);
        persist();
    }

    @Override
    public Optional<Account> findById(String accountNumber) {
        return Optional.ofNullable(store.get(accountNumber));
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(String accountNumber) {
        store.remove(accountNumber);
        persist();
    }

    @Override
    public boolean exists(String accountNumber) {
        return store.containsKey(accountNumber);
    }

    // ── Custom queries (Streams + Lambdas) ─────────────────────────────────────
    public List<Account> findByOwnerName(String name) {
        return store.values().stream()
                .filter(a -> a.getOwnerName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }

    public List<Account> findFrozenAccounts() {
        return store.values().stream()
                .filter(Account::isFrozen)
                .collect(Collectors.toList());
    }

    public double getTotalDeposits() {
        return store.values().stream()
                .mapToDouble(Account::getBalance)
                .sum();
    }

    private void persist() {
        fileManager.save(FILE_NAME, (HashMap<String, Account>) store);
    }
}