export default function HowItWorks() {
  return (
    <div>
      <div className="page-header">
        <h2>📖 原理说明</h2>
        <p>OntoEvolve 框架的完整工作流程与技术详解</p>
      </div>

      {/* 概述 */}
      <div className="card mb-6">
        <div className="card-title mb-4">系统概述</div>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14, lineHeight: 1.8 }}>
          <strong style={{ color: 'var(--text)' }}>OntoEvolve</strong>（Ontology-Driven Self-Evolving Decision Framework）
          是一个<strong>本体驱动的自进化决策框架</strong>。它将<strong>语义技术（OWL 本体）</strong>与
          <strong>进化计算</strong>相结合，构建了一套"分类 → 匹配 → 进化 → 反馈"的闭环决策系统。
        </p>
        <div className="explain-box mt-4">
          <p>
            <strong>核心理念：</strong>每个行为问题都是一个"生态位"（Niche），
            每个生态位中维护着一个"决策方案种群"（Population）。
            种群通过进化算法代际迭代，持续优化方案质量。
            用一句话概括：<strong>让决策方案像生物一样进化</strong>。
          </p>
        </div>
      </div>

      {/* 五大环节 */}
      <h3 className="mb-4" style={{ fontSize: 18 }}>五大核心环节</h3>

      <div className="stepper mb-6">
        <div className="stepper-step">
          <div className="stepper-number">1</div>
          <div className="stepper-content">
            <h4>📥 输入事件（Input Event）</h4>
            <p>
              系统的起点。来自教育场景的学生行为描述以自然语言文本形式进入系统。
              例如："学生S001在课堂上大声喧哗，多次打断老师讲课"。
              事件携带学生ID、地点、严重程度等上下文信息。
            </p>
            <div className="code-block" style={{ marginTop: 8, fontSize: 12 }}>
{`// 核心模型
public class InputEvent {
    String id;              // 事件唯一标识
    Instant timestamp;      // 发生时间
    String rawDescription;  // 原始行为描述（自然语言）
    String subjectId;       // 学生ID
    Map<String, Object> attributes;  // 扩展属性
}`}
            </div>
          </div>
        </div>

        <div className="stepper-step">
          <div className="stepper-number">2</div>
          <div className="stepper-content">
            <h4>🏷️ LLM 语义分类（Classification）</h4>
            <p>
              利用 <strong>DeepSeek LLM</strong> 对自然语言描述进行语义理解，
              将事件映射到教育本体（education.ttl）中定义的 <strong>ActionType</strong> 概念节点。
              例如，"课堂喧哗" → <code>ClassroomDisruption</code>（edu:ClassroomDisruption）。
            </p>
            <div className="explain-box" style={{ marginTop: 8 }}>
              <p>
                <strong>为什么用 LLM？</strong> 传统规则分类器无法覆盖自然语言的多样性。
                LLM 可以理解"在班级群里发恶意P图" ≠ "同学冲突"，
                而是 <strong>网络欺凌（Cyberbullying）</strong>，这是关键词匹配无法做到的。
              </p>
            </div>
            <div className="code-block" style={{ marginTop: 8, fontSize: 12 }}>
{`// LLM 分类器
public class LLMActionClassifier implements Classifier<ActionEvent, ActionType> {
    public ActionType classify(ActionEvent event) {
        String prompt = buildPrompt(event);  // 构建设计好的 prompt
        String llmResponse = llmClient.generate(prompt);  // 调用 DeepSeek
        return resolveActionType(llmResponse, event);  // 解析结果
    }
}`}
            </div>
          </div>
        </div>

        <div className="stepper-step">
          <div className="stepper-number">3</div>
          <div className="stepper-content">
            <h4>🎯 方案匹配（Matching）</h4>
            <p>
              分类确定行为类型后，从该生态位的种群中 <strong>匹配最优干预方案</strong>。
              使用 <strong>Pareto + UCB</strong> 混合策略：
              对多维评分向量进行 Pareto 排序，叠加 UCB 探索奖励，
              在"利用已知好的方案"和"探索未充分测试的方案"之间取得平衡。
            </p>
            <div className="code-block" style={{ marginTop: 8, fontSize: 12 }}>
{`// Pareto + UCB 混合匹配
private double score(Assignment a, int totalTrials) {
    double paretoScore = normalizeScore(a.getScoreVector());
    double ucbBonus = explorationBonus
        * Math.sqrt(2 * Math.log(totalTrials) / a.getTrials());
    return paretoScore + ucbBonus;  // 利用 + 探索
}`}
            </div>
          </div>
        </div>

        <div className="stepper-step">
          <div className="stepper-number">4</div>
          <div className="stepper-content">
            <h4>🧬 进化优化（Evolution）</h4>
            <p>
              核心创新。每个生态位维护一个 <strong>DecisionPopulation</strong>，
              通过三种变异算子产生新方案，通过 Pareto 选择算子淘汰劣质方案，
              通过迁移算子在生态位间共享优秀方案。
            </p>
            <div style={{ display: 'flex', gap: 12, marginTop: 8, flexWrap: 'wrap' }}>
              <div className="card" style={{ flex: 1, minWidth: 180 }}>
                <div style={{ fontSize: 20, marginBottom: 6 }}>🤖</div>
                <div style={{ fontWeight: 600, fontSize: 14 }}>LLM 生成</div>
                <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                  利用 LLM 基于成功方案特征生成全新方案
                </div>
              </div>
              <div className="card" style={{ flex: 1, minWidth: 180 }}>
                <div style={{ fontSize: 20, marginBottom: 6 }}>🔀</div>
                <div style={{ fontWeight: 600, fontSize: 14 }}>交叉重组</div>
                <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                  选择两个高分亲本，由 LLM 融合生成新方案
                </div>
              </div>
              <div className="card" style={{ flex: 1, minWidth: 180 }}>
                <div style={{ fontSize: 20, marginBottom: 6 }}>🔧</div>
                <div style={{ fontWeight: 600, fontSize: 14 }}>微扰变异</div>
                <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                  对现有方案进行小幅参数调整（不依赖 LLM）
                </div>
              </div>
            </div>
            <div className="code-block" style={{ marginTop: 8, fontSize: 12 }}>
{`// 进化引擎核心循环
public void runFullEvolution(Concept concept) {
    // 1. 变异：按权重选择变异算子生成新方案
    Variator variator = selectVariatorByWeight();
    List<Assignment> newborns = variator.generate(ctx);

    // 2. 本体验证：确保新方案符合 OWL 约束
    for (Assignment newborn : newborns) {
        if (ontologyValidator.validate(newborn))
            pop.addMember(newborn);
    }

    // 3. 选择：Pareto 排序淘汰劣质方案
    List<Assignment> selected = selector.select(candidates, maxSize);
    pop.replaceMembers(selected);

    // 4. 迁移：精英方案跨生态位共享
    if (migrationEnabled) checkAndMigrate(concept, pop);
}`}
            </div>
          </div>
        </div>

        <div className="stepper-step">
          <div className="stepper-number">5</div>
          <div className="stepper-content">
            <h4>📊 反馈驱动（Feedback）</h4>
            <p>
              每次干预执行后，系统收集 <strong>三维反馈向量</strong>：
              效果（effectiveness）、成本（cost）、满意度（satisfaction）。
              反馈通过 <strong>Welford 在线算法</strong> 更新方案的统计量，
              累积到阈值时自动触发种群重排，形成闭环进化。
            </p>
            <div className="code-block" style={{ marginTop: 8, fontSize: 12 }}>
{`// Welford 在线更新算法
public void updateScore(double[] feedback) {
    trials++;
    for (int i = 0; i < scoreVector.length; i++) {
        double delta = feedback[i] - scoreVector[i];
        scoreVector[i] += delta / trials;  // 增量均值
        varianceVector[i] += delta * (feedback[i] - scoreVector[i]);  // 增量方差
    }
}

// 反馈阈值触发进化
public void recordFeedback(Concept concept) {
    feedbackCounters.merge(concept.getIri(), 1, Integer::sum);
    if (pop.isEvolutionDue(feedbackCount)) {
        runLightEvolution(concept);  // 自动触发重排
    }
}`}
            </div>
          </div>
        </div>
      </div>

      {/* 本体层次 */}
      <div className="card mb-6">
        <div className="card-title mb-4">🌳 教育本体层次（Education Ontology）</div>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 16 }}>
          使用 OWL 本体语言定义的行为分类体系，位于 <code>education.ttl</code> 文件中。
          所有分类和推理均基于此本体。
        </p>
        <div className="grid grid-3">
          <div className="card" style={{ background: 'rgba(182,106,80,0.05)', borderColor: 'rgba(182,106,80,0.2)' }}>
            <h4 style={{ color: '#B66A50', marginBottom: 8 }}>行为问题 Behavioral</h4>
            <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
              课堂扰乱 · 同学冲突 · 不服从管理 · 网络欺凌 · 逃课旷课 · 吸烟饮酒
            </div>
          </div>
          <div className="card" style={{ background: 'rgba(57,81,65,0.05)', borderColor: 'rgba(57,81,65,0.2)' }}>
            <h4 style={{ color: '#395141', marginBottom: 8 }}>学业问题 Academic</h4>
            <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
              作业不交 · 考试作弊 · 成绩下滑 · 课堂走神 · 迟交作业
            </div>
          </div>
          <div className="card" style={{ background: 'rgba(112,128,112,0.05)', borderColor: 'rgba(112,128,112,0.2)' }}>
            <h4 style={{ color: '#708070', marginBottom: 8 }}>社交问题 Social</h4>
            <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
              社交退缩 · 破坏公物
            </div>
          </div>
        </div>
      </div>

      {/* 干预方案 */}
      <div className="card mb-6">
        <div className="card-title mb-4">💊 干预方案目录</div>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 16 }}>
          系统内置六类干预方案模板，进化引擎在此基础上产生变异方案。
        </p>
        <div className="grid grid-3">
          {[
            { type: 'talk', name: '谈话教育', desc: '一对一谈话，了解情况并引导改进', icon: '💬' },
            { type: 'notice', name: '通知家长', desc: '电话或面谈通知家长，协同教育', icon: '📞' },
            { type: 'activity', name: '活动引导', desc: '通过课外活动引导行为改善', icon: '🎯' },
            { type: 'reward', name: '奖励机制', desc: '正向激励强化良好行为', icon: '🏆' },
            { type: 'counseling', name: '心理辅导', desc: '转介心理老师进行专业干预', icon: '🧠' },
            { type: 'academic', name: '学业辅导', desc: '课后补习或学习支持计划', icon: '📚' },
          ].map(item => (
            <div key={item.type} className="card" style={{ padding: 16 }}>
              <div style={{ fontSize: 24, marginBottom: 6 }}>{item.icon}</div>
              <div style={{ fontWeight: 600, fontSize: 14 }}>{item.name}</div>
              <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{item.desc}</div>
            </div>
          ))}
        </div>
      </div>

      {/* 数据源 */}
      <div className="card">
        <div className="card-title mb-4">📊 数据来源</div>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14, lineHeight: 1.8 }}>
          学生数据来自 <strong>UCI Student Performance 数据集</strong>
          （Cortez & Silva, 2008），包含两门课程的学生信息：
        </p>
        <ul style={{ color: 'var(--text-secondary)', fontSize: 14, lineHeight: 2, paddingLeft: 20, marginTop: 8 }}>
          <li><strong>数学课（student-mat.csv）</strong> — 395 名学生，33 个属性</li>
          <li><strong>葡萄牙语课（student-por.csv）</strong> — 649 名学生，33 个属性</li>
          <li>属性包括：人口学信息（年龄、性别、住址）、社会学信息（家庭教育背景、父母职业）、学业信息（成绩、缺勤、学习时间）</li>
        </ul>
        <div className="explain-box mt-4">
          <p>
            用户的行为事件脚本（<code>generate-test-events.py</code>）基于学生真实特征
            （缺勤数、过往挂科数、健康状况等）生成有针对性的行为事件，
            使测试数据更加真实可信。
          </p>
        </div>
      </div>
    </div>
  );
}
