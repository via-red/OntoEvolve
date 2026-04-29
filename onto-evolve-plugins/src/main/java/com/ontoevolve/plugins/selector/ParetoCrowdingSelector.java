package com.ontoevolve.plugins.selector;

import com.ontoevolve.core.model.Assignment;
import com.ontoevolve.core.spi.Selector;

import java.util.*;

/**
 * Pareto 拥挤度选择算子 — NSGA-II 风格的多目标选择。
 * <p>
 * 流程：
 * 1. 按多目标反馈向量做 Pareto 支配排序，分层
 * 2. 在同一层内按拥挤距离排序，保留稀疏区域的方案
 * 3. 从高到低截断到种群容量 K
 * <p>
 * 这保证了多维权衡不被压缩为单一分数，同时维持了种群多样性。
 */
public class ParetoCrowdingSelector implements Selector<Assignment> {

    @Override
    public List<Assignment> select(List<Assignment> candidates, int capacity) {
        if (candidates.isEmpty()) return List.of();
        if (candidates.size() <= capacity) return candidates;

        List<List<Assignment>> fronts = fastNonDominatedSort(candidates);
        List<Assignment> selected = new ArrayList<>();

        for (List<Assignment> front : fronts) {
            if (selected.size() + front.size() <= capacity) {
                selected.addAll(front);
            } else {
                // 最后前沿按拥挤距离排序
                double[] crowding = calculateCrowdingDistance(front);
                List<Assignment> sortedFront = new ArrayList<>(front);
                sortedFront.sort(Comparator.comparingDouble(
                        (Assignment a) -> crowding[front.indexOf(a)]
                ).reversed());

                int remaining = capacity - selected.size();
                selected.addAll(sortedFront.subList(0, remaining));
                break;
            }
        }
        return selected;
    }

    /**
     * 快速非支配排序 (Fast Non-Dominated Sort)。
     * 返回从高到低的前沿分层。
     */
    private List<List<Assignment>> fastNonDominatedSort(List<Assignment> population) {
        List<List<Assignment>> fronts = new ArrayList<>();
        fronts.add(new ArrayList<>()); // 前沿 0

        Map<Assignment, Set<Assignment>> dominatedSet = new HashMap<>();
        Map<Assignment, Integer> dominationCount = new HashMap<>();
        List<Assignment> all = new ArrayList<>(population);

        for (Assignment p : all) {
            dominatedSet.put(p, new HashSet<>());
            dominationCount.put(p, 0);
        }

        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                Assignment p = all.get(i);
                Assignment q = all.get(j);
                if (dominates(p, q)) {
                    dominatedSet.get(p).add(q);
                    dominationCount.merge(q, 1, Integer::sum);
                } else if (dominates(q, p)) {
                    dominatedSet.get(q).add(p);
                    dominationCount.merge(p, 1, Integer::sum);
                }
            }
        }

        for (Assignment p : all) {
            if (dominationCount.get(p) == 0) {
                fronts.get(0).add(p);
            }
        }

        int i = 0;
        while (!fronts.get(i).isEmpty()) {
            List<Assignment> nextFront = new ArrayList<>();
            for (Assignment p : fronts.get(i)) {
                for (Assignment q : dominatedSet.get(p)) {
                    dominationCount.merge(q, -1, Integer::sum);
                    if (dominationCount.get(q) == 0) {
                        nextFront.add(q);
                    }
                }
            }
            i++;
            fronts.add(nextFront);
        }

        fronts.removeIf(List::isEmpty);
        return fronts;
    }

    /** p 支配 q: p 在所有维度上不差于 q，且至少一个维度严格优于 q */
    private boolean dominates(Assignment p, Assignment q) {
        double[] pScore = p.getScoreVector();
        double[] qScore = q.getScoreVector();
        boolean betterInAny = false;
        for (int i = 0; i < pScore.length; i++) {
            if (pScore[i] < qScore[i]) return false; // p 在维度 i 上更差
            if (pScore[i] > qScore[i]) betterInAny = true;
        }
        return betterInAny;
    }

    /**
     * 计算同一前沿内个体的拥挤距离。
     * 拥挤距离越大，该个体周围越稀疏，越应优先保留。
     */
    private double[] calculateCrowdingDistance(List<Assignment> front) {
        int n = front.size();
        int dim = front.get(0).getScoreVector().length;
        double[] distance = new double[n];

        for (int m = 0; m < dim; m++) {
            final int d = m;
            front.sort(Comparator.comparingDouble(a -> a.getScoreVector()[d]));

            distance[0] = Double.MAX_VALUE;
            distance[n - 1] = Double.MAX_VALUE;

            double min = front.get(0).getScoreVector()[m];
            double max = front.get(n - 1).getScoreVector()[m];
            double range = max - min;
            if (range == 0) range = 1;

            for (int i = 1; i < n - 1; i++) {
                distance[i] += (front.get(i + 1).getScoreVector()[m]
                        - front.get(i - 1).getScoreVector()[m]) / range;
            }
        }

        return distance;
    }
}
