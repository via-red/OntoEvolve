# OntoEvolve 测试数据

## 目录结构

```
data/
├── README.md                          # 本文件
├── generate-test-events.py            # 测试事件生成脚本
├── ontologies/                        # 下载的 OWL 本体文件
│   ├── pizza.owl                      # Protege Pizza Ontology — OWL 推理测试
│   ├── SUMO.owl                       # SUMO 上层本体 — 大规模本体测试
│   ├── foaf.rdf                       # FOAF 本体 — 基础 RDF 解析测试
│   └── vc-db-1.rdf                   # Apache Jena 教程示例数据
├── student-data/                      # UCI 学生表现数据集
│   ├── student-mat.csv                # 数学课 (395 学生 × 33 属性)
│   ├── student-por.csv                # 葡萄牙语课 (649 学生 × 33 属性)
│   ├── student-merge.R                # 合并脚本
│   └── student.txt                    # 数据字典
└── test-events/                       # 生成的测试事件（可选）
```

## 数据集说明

### 1. 教育 OWL 本体
- **位置**: `onto-domain-education/src/main/resources/ontology/education.ttl`
- **命名空间**: `http://ontoevolve/education#`
- **概念层次**:
  - `Behavioral` → 课堂扰乱, 同学冲突, 不服从管理, 网络欺凌, 逃课旷课, 吸烟饮酒
  - `Academic` → 作业不交, 考试作弊, 成绩下滑, 课堂走神, 迟交作业
  - `Social` → 社交退缩, 破坏公物
- **干预方案**: 谈话教育, 通知家长, 活动引导, 奖励机制, 心理辅导, 学业辅导
- **反馈维度**: effectiveness, cost, satisfaction, longTermEffect

### 2. UCI Student Performance 数据集
- **来源**: Paulo Cortez, University of Minho
- **DOI**: 10.24432/C5TG7T
- **引用**: Dua, D. and Graff, C. (2019). UCI Machine Learning Repository
- **特征**: 33 维（人口学、社会学、学业特征）
- **适用场景**: 基于真实学生档案生成事件 → 测试分类/匹配/进化

### 3. 通用 OWL 本体
| 文件 | 大小 | 用途 | 来源 |
|------|------|------|------|
| `pizza.owl` | ~160KB | OWL 推理测试（经典教学本体）| Protege 项目 |
| `SUMO.owl` | ~300KB | 大规模本体加载/查询性能测试 | ontologyportal/sumo |
| `foaf.rdf` | ~21KB | 基础 RDF 解析 | W3C |
| `vc-db-1.rdf` | ~1KB | Jena API 快速验证 | Apache Jena |

## 使用方法

### 生成测试事件
```bash
# 生成 50 条事件 JSON
python data/generate-test-events.py --count 50 > events.json

# 生成 curl 批量导入脚本（含反馈）
python data/generate-test-events.py --count 30 --api --include-feedback > import.sh
bash import.sh

# 仅生成事件（不含反馈）
python data/generate-test-events.py --count 100 --api > import-events.sh
```

### 替换本体文件
```yaml
# application.yml 配置
onto:
  rdf:
    ontology-path: classpath:ontology/education.ttl   # 当前路径
    # 使用 SUMO 测试:
    # ontology-path: file:data/ontologies/SUMO.owl
```

### 使用其他 OWL 本体测试
```bash
# 测试 Pizza Ontology 的 OWL-DL 推理
cp data/ontologies/pizza.owl onto-domain-education/src/main/resources/ontology/

# 或修改配置指向本地路径
# onto.rdf.ontology-path: file:data/ontologies/pizza.owl
```

## 硬件要求

| 本体 | 大小 | ~三元组数 | 建议用途 |
|------|------|-----------|----------|
| education.ttl | ~4KB | ~80 | 日常开发/单元测试 |
| pizza.owl | 160KB | ~2000 | OWL 推理测试 |
| SUMO.owl | 300KB | ~25000 | 性能压测/压力测试 |
