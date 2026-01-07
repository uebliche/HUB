package net.uebliche.hub.common.storage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import java.util.UUID;

public final class MongoJumpRunScoreRepository implements JumpRunScoreRepository {
    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoJumpRunScoreRepository(StorageSettings.MongoSettings settings) {
        if (settings.uri() == null || settings.uri().isBlank()) {
            throw new StorageException("Mongo uri is not configured");
        }
        if (settings.database() == null || settings.database().isBlank()) {
            throw new StorageException("Mongo database is not configured");
        }
        this.client = MongoClients.create(settings.uri());
        String prefix = settings.collectionPrefix() == null ? "hub_" : settings.collectionPrefix();
        this.collection = client.getDatabase(settings.database()).getCollection(prefix + "jump_run_scores");
        this.collection.createIndex(Indexes.compoundIndex(Indexes.ascending("courseId"), Indexes.ascending("playerId")));
    }

    @Override
    public JumpRunScore getScore(String courseId, UUID playerId) {
        Document doc = collection.find(Filters.and(
                Filters.eq("courseId", courseId),
                Filters.eq("playerId", playerId.toString())
        )).first();
        if (doc == null) {
            return null;
        }
        long runCount = doc.getLong("runCount") == null ? 0L : doc.getLong("runCount");
        long bestTime = doc.getLong("bestTimeMs") == null ? 0L : doc.getLong("bestTimeMs");
        long updatedAt = doc.getLong("updatedAt") == null ? 0L : doc.getLong("updatedAt");
        return new JumpRunScore(courseId, playerId, runCount, bestTime, updatedAt);
    }

    @Override
    public JumpRunScore recordRun(String courseId, UUID playerId, long timeMillis) {
        Document existing = collection.find(Filters.and(
                Filters.eq("courseId", courseId),
                Filters.eq("playerId", playerId.toString())
        )).first();
        long now = System.currentTimeMillis();
        long runCount;
        long bestTime;
        if (existing == null) {
            runCount = 1L;
            bestTime = timeMillis > 0 ? timeMillis : 0L;
            Document doc = new Document()
                    .append("courseId", courseId)
                    .append("playerId", playerId.toString())
                    .append("runCount", runCount)
                    .append("bestTimeMs", bestTime)
                    .append("updatedAt", now);
            collection.insertOne(doc);
        } else {
            runCount = (existing.getLong("runCount") == null ? 0L : existing.getLong("runCount")) + 1L;
            bestTime = existing.getLong("bestTimeMs") == null ? 0L : existing.getLong("bestTimeMs");
            if (timeMillis > 0 && (bestTime <= 0 || timeMillis < bestTime)) {
                bestTime = timeMillis;
            }
            Document update = new Document()
                    .append("runCount", runCount)
                    .append("bestTimeMs", bestTime)
                    .append("updatedAt", now);
            collection.updateOne(
                    Filters.and(Filters.eq("courseId", courseId), Filters.eq("playerId", playerId.toString())),
                    new Document("$set", update)
            );
        }
        return new JumpRunScore(courseId, playerId, runCount, bestTime, now);
    }

    @Override
    public void close() {
        client.close();
    }
}
