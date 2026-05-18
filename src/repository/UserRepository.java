package repository;

import models.User;
import utils.FileManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UserRepository implements Repository<User, String> {

    private static final String FILE_NAME = "users.dat";
    private final Map<String, User> store;
    private final FileManager fileManager;

    @SuppressWarnings("unchecked")
    public UserRepository(FileManager fileManager) {
        this.fileManager = fileManager;
        Map<String, User> loaded = (Map<String, User>) fileManager.load(FILE_NAME);
        this.store = (loaded != null) ? loaded : new HashMap<>();
    }

    @Override
    public void save(User user) {
        store.put(user.getUserId(), user);
        persist();
    }

    @Override
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(String userId) {
        store.remove(userId);
        persist();
    }

    @Override
    public boolean exists(String userId) {
        return store.containsKey(userId);
    }

    public Optional<User> findByEmail(String email) {
        return store.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    private void persist() {
        fileManager.save(FILE_NAME, (HashMap<String, User>) store);
    }
}