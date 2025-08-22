package org.SouthMillion.dto.drop;


import lombok.Getter;
import java.util.*;

@Getter
public class CompiledDrop {
    public static record Row(int itemId, int num, int bind, int broadcast, int weight) {}

    private final int dropId;
    private final List<Row> rows;
    private final int totalWeight;
    private final int[] prefix;

    public CompiledDrop(DropXml x) {
        this.dropId = x.getDropId();
        List<Row> r = new ArrayList<>();
        int sum = 0;
        for (var it : x.getDropItemProbList().getItems()) {
            if (it.getProb() <= 0) continue;
            r.add(new Row(it.getItemId(), it.getNum(), it.getIsBind(), it.getBroadcast(), it.getProb()));
            sum += it.getProb();
        }
        if (r.isEmpty() || sum <= 0) throw new IllegalArgumentException("drop "+dropId+" has no positive weight");
        this.rows = List.copyOf(r);
        this.totalWeight = sum;
        this.prefix = new int[rows.size()];
        int acc = 0;
        for (int i=0;i<rows.size();i++) {
            acc += rows.get(i).weight();
            prefix[i]=acc;
        }
    }

    public Row pick(Random rnd) {
        int r = 1 + rnd.nextInt(totalWeight);
        int i = Arrays.binarySearch(prefix, r);
        if (i < 0) i = -i - 1;
        return rows.get(i);
    }

    public Row pickRareOnly(Random rnd, List<Integer> rareListOverride) {
        List<Row> pool = (rareListOverride!=null && !rareListOverride.isEmpty())
                ? rows.stream().filter(row -> rareListOverride.contains(row.itemId())).toList()
                : rows.stream().filter(row -> row.broadcast()==1).toList();
        if (pool.isEmpty()) return pick(rnd);
        int sum = pool.stream().mapToInt(Row::weight).sum();
        int r = 1 + rnd.nextInt(sum);
        int acc = 0;
        for (Row row : pool) { acc += row.weight(); if (acc >= r) return row; }
        return pool.getLast();
    }
}