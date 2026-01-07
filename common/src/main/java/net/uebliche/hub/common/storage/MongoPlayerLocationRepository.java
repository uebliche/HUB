package net.uebliche.hub.common.storage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import java.util.UUID;

public final class MongoPlayerLocationRepository implements PlayerLocationRepository {
    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoPlayerLocationRepository(StorageSettings.MongoSettings settings) {
        if (settings.uri() == null || settings.uri().isBlank()) {
            throw new StorageException("Mongo uri is not configured");
        }
        if (settings.database() == null || settings.database().isBlank()) {
            throw new StorageException("Mongo database is not configured");
        }
        this.client = MongoClients.create(settings.uri());
        String prefix = settings.collectionPrefix() == null ? "hub_" : settings.collectionPrefix();
        this.collection = client.getDatabase(settings.database()).getCollection(prefix + "player_locations");
        this.collection.createIndex(Indexes.compoundIndex(Indexes.ascending("serverId"), Indexes.ascending("playerId")));
    }

    @Override
    public PlayerLocation getLocation(String serverId, UUID playerId) {
        Document doc = collection.find(Filters.and(
                Filters.eq("serverId", serverId),
                Filters.eq("playerId", playerId.toString())
        )).first();
        if (doc == null) {
            return null;
        }
        return new PlayerLocation(
                serverId,
                playerId,
                doc.getString("world"),
                getDouble(doc, "x"),
                getDouble(doc, "y"),
                getDouble(doc, "z"),
                getFloat(doc, "yaw"),
                getFloat(doc, "pitch"),
                getLong(doc, "updatedAt")
        );
    }

    @Override
    public void saveLocation(PlayerLocation location) {
        Document update = new Document()
                .append("serverId", location.serverId())
                .append("playerId", location.playerId().toString())
                .append("world", location.world())
                .append("x", location.x())
                .append("y", location.y())
                .append("z", location.z())
                .append("yaw", location.yaw())
                .append("pitch", location.pitch())
                .append("updatedAt", location.updatedAt());
        collection.updateOne(
                Filters.and(Filters.eq("serverId", location.serverId()), Filters.eq("playerId", location.playerId().toString())),
                new Document("$set", update),
                new com.mongodb.client.model.UpdateOptions().upsert(true)
        );
    }

    @Override
    public void close() {
        client.close();
    }

    private static long getLong(Document doc, String key) {
        Long value = doc.getLong(key);
        return value == null ? 0L : value;
    }

    private static double getDouble(Document doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0D;
    }

    private static float getFloat(Document doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return 0.0F;
    }
}
