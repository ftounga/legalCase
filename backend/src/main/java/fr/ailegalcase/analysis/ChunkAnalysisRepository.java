package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChunkAnalysisRepository extends JpaRepository<ChunkAnalysis, UUID> {

    List<ChunkAnalysis> findByChunkId(UUID chunkId);
}
