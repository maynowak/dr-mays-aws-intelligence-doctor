package io.github.maynowak.drmays.api.v1;

/**
 * Summary statistics for a graph response.
 *
 * @param nodeCount total nodes
 * @param edgeCount total edges
 * @param inventoriedCount inventoried nodes
 * @param referencedCount referenced nodes
 * @param componentCount undirected connected components
 * @param cyclic whether a directed cycle exists
 */
public record GraphStatsResponse(
    int nodeCount,
    int edgeCount,
    int inventoriedCount,
    int referencedCount,
    int componentCount,
    boolean cyclic) {}
