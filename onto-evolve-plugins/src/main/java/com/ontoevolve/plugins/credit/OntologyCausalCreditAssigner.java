package com.ontoevolve.plugins.credit;

import com.ontoevolve.core.model.Execution;
import com.ontoevolve.core.model.Feedback;
import com.ontoevolve.core.spi.CreditAssigner;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本体因果链信用分配器。
 * <p>
 * 利用本体中定义的因果链属性（如 Intervention → BehaviorChange → Outcome），
 * 将延迟到达的长期反馈沿因果路径回溯分配到相关的历史 Execution。
 * <p>
 * 分配权重由时间衰减和因果距离共同决定。
 */
public class OntologyCausalCreditAssigner implements CreditAssigner {

    private final double lambda;       // 衰减因子
    private final int maxLookbackDays; // 最大回溯天数

    public OntologyCausalCreditAssigner(double lambda, int maxLookbackDays) {
        this.lambda = lambda;
        this.maxLookbackDays = maxLookbackDays;
    }

    public OntologyCausalCreditAssigner() {
        this(0.9, 30);
    }

    @Override
    public List<Feedback> distribute(Feedback feedback, List<Execution> history) {
        List<Feedback> distributed = new ArrayList<>();
        distributed.add(feedback); // 原始反馈保留

        Instant feedbackTime = feedback.getTimestamp();
        long cutoff = feedbackTime.minus(Duration.ofDays(maxLookbackDays)).toEpochMilli();

        for (Execution exec : history) {
            long execTime = exec.getExecutedAt().toEpochMilli();
            if (execTime < cutoff) continue;

            // 时间衰减权重
            double timeWeight = Math.pow(lambda,
                    Duration.between(exec.getExecutedAt(), feedbackTime).toDays());

            if (timeWeight < 0.01) continue;

            // 派生反馈：给历史执行分配一部分信用
            double[] allocatedScores = new double[feedback.getDimension()];
            for (int i = 0; i < feedback.getDimension(); i++) {
                allocatedScores[i] = feedback.getScores()[i] * timeWeight * 0.5;
            }

            Feedback derived = new Feedback(
                    "credit:" + feedback.getIri() + ":exec:" + exec.getIri(),
                    exec,
                    allocatedScores,
                    Map.of("sourceFeedback", feedback.getIri(),
                           "timeWeight", timeWeight,
                           "assigner", "OntologyCausal")
            );
            distributed.add(derived);
        }

        return distributed;
    }
}
