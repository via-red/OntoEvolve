#!/usr/bin/env python3
"""
OntoEvolve 测试事件生成器
=========================
基于 UCI Student Performance 数据集，生成模拟学生行为事件，
用于测试 OntoEvolve 系统的分类→匹配→反馈→进化全流程。

用法:
  python generate-test-events.py                     # 输出到 stdout
  python generate-test-events.py --count 50 > events.json   # 保存到文件
  python generate-test-events.py --api               # 输出 curl 命令
"""

import csv
import json
import random
import argparse
from datetime import datetime, timedelta

# ============================================================
# 学生数据
# ============================================================
def load_students(mat_path, por_path):
    """加载 UCI Student Performance 数据集中的学生信息."""
    students = []
    for path in [mat_path, por_path]:
        try:
            with open(path, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f, delimiter=';')
                for row in reader:
                    students.append({
                        'school': row['school'],
                        'sex': row['sex'],
                        'age': int(row['age']),
                        'address': row['address'],
                        'studytime': int(row['studytime']),
                        'failures': int(row['failures']),
                        'absences': int(row['absences']),
                        'health': int(row['health']),
                        'g1': row.get('G1', '0'),
                        'g2': row.get('G2', '0'),
                        'g3': row.get('G3', '0'),
                    })
        except FileNotFoundError:
            continue
    return students

# ============================================================
# 行为事件模板
# ============================================================
BEHAVIOR_EVENTS = [
    # (concept_iri, behavior_description, severity)
    # --- 行为问题 ---
    ("ClassroomDisruption", "上课大声喧哗，多次打断老师讲课", "moderate"),
    ("ClassroomDisruption", "在课堂上随意走动，影响其他同学听课", "moderate"),
    ("ClassroomDisruption", "上课玩手机被老师发现", "mild"),
    ("PeerConflict", "与同桌发生争吵，互相推搡", "moderate"),
    ("PeerConflict", "课间因小事与同学打架", "severe"),
    ("PeerConflict", "在社交媒体上辱骂同班同学", "moderate"),
    ("Noncompliance", "拒绝按照老师要求调换座位", "mild"),
    ("Noncompliance", "不交手机，与检查老师发生冲突", "moderate"),
    ("Noncompliance", "多次无视课堂纪律要求", "mild"),
    ("Cyberbullying", "在班级群里发布同学的恶意P图", "severe"),
    ("Cyberbullying", "在社交平台散布关于同学的谣言", "severe"),
    ("Cyberbullying", "网络游戏中对同学进行言语攻击", "moderate"),
    ("Truancy", "未经请假缺课，被班主任发现", "moderate"),
    ("Truancy", "连续多日旷课", "severe"),
    ("SubstanceMisuse", "课间被发现躲在厕所吸烟", "severe"),
    ("SubstanceMisuse", "参加校外饮酒聚会", "severe"),
    # --- 学业问题 ---
    ("HomeworkMissing", "连续三天未交数学作业", "mild"),
    ("HomeworkMissing", '以"忘记带"为由多次不交作业', "mild"),
    ("Cheating", "期中考试抄袭邻座答案被监考老师发现", "severe"),
    ("Cheating", "小测验中使用手机查答案", "moderate"),
    ("LowPerformance", "月考成绩从85分降至60分", "moderate"),
    ("LowPerformance", "期中考试多门不及格", "severe"),
    ("Inattention", "上课经常走神发呆，笔记空白", "mild"),
    ("Inattention", "课堂上打瞌睡被老师叫醒多次", "mild"),
    ("LateSubmission", "项目报告晚提交两天", "mild"),
    ("LateSubmission", "多次迟交小组作业影响团队进度", "moderate"),
    # --- 社交问题 ---
    ("SocialWithdrawal", "拒绝参加班级集体活动", "mild"),
    ("SocialWithdrawal", "课间总是一个人独处，不与同学交流", "mild"),
    ("DisruptiveBehavior", "故意踢坏教室门", "severe"),
    ("DisruptiveBehavior", "在课桌上乱涂乱画甚至刻字", "moderate"),
]

INTERVENTIONS = [
    {"type": "talk", "name": "一对一谈话", "steps": ["了解事情经过", "倾听学生解释", "指出行为的不当之处", "共同约定改进目标"]},
    {"type": "talk", "name": "严肃约谈", "steps": ["单独约谈学生", "明确违反的校规条款", "告知后果", "签署行为承诺书"]},
    {"type": "notice", "name": "电话通知家长", "steps": ["电话联系家长", "说明学生在校表现", "建议家庭教育配合", "约定后续沟通频率"]},
    {"type": "notice", "name": "家长面谈", "steps": ["邀请家长到校面谈", "共同分析问题原因", "制定家校协同方案", "定期反馈学生进展"]},
    {"type": "activity", "name": "集体活动引导", "steps": ["安排参与班级服务岗位", "通过合作任务增进同学关系", "定期给予正向反馈"]},
    {"type": "activity", "name": "兴趣小组参与", "steps": ["了解学生兴趣方向", "推荐参与相关社团或小组", "鼓励展示特长并获得认可"]},
    {"type": "reward", "name": "行为积分奖励", "steps": ["设立行为改善目标", "每日记录进步情况", "达到目标给予班级表彰", "累积积分兑换奖励"]},
    {"type": "reward", "name": "榜样示范激励", "steps": ["发现闪光点及时表扬", "安排担任课代表或小组长", "公开表彰进步"]},
    {"type": "counseling", "name": "心理辅导转介", "steps": ["与心理老师沟通情况", "安排定期心理咨询", "关注学生情绪变化", "与家长保持沟通"]},
    {"type": "academic", "name": "课后补习计划", "steps": ["摸底薄弱科目", "制定补习计划", "安排同学结对帮扶", "每周检查学习进展"]},
    {"type": "academic", "name": "学习习惯指导", "steps": ["分析学习方法问题", "制定作息时间表", "教授笔记和复习技巧", "跟踪执行情况"]},
]

LOCATIONS = ["教室", "操场", "食堂", "图书馆", "宿舍", "走廊", "实验室", "机房", "校园外"]

# ============================================================
# 事件生成
# ============================================================
def generate_student_id(student, idx):
    """根据学生数据生成风格一致的 ID."""
    prefix = student['school']  # GP=Gabriel Pereira, MS=Mousinho da Silveira
    gender = 'M' if student['sex'] == 'M' else 'F'
    return f"S{prefix}{gender}{idx:04d}"

def make_behavior_text(student, student_id):
    """结合学生特征生成更真实的行为事件."""
    # 根据学生特征选择合适的事件类型
    if student['failures'] > 2 and random.random() < 0.4:
        events = [e for e in BEHAVIOR_EVENTS if e[0] in ('HomeworkMissing', 'Cheating', 'LowPerformance')]
    elif student['absences'] > 10 and random.random() < 0.4:
        events = [e for e in BEHAVIOR_EVENTS if e[0] in ('Truancy', 'Noncompliance')]
    elif student['health'] < 3 and random.random() < 0.3:
        events = [e for e in BEHAVIOR_EVENTS if e[0] in ('Inattention', 'SocialWithdrawal')]
    else:
        events = BEHAVIOR_EVENTS

    concept_label, description, severity = random.choice(events)

    # 增加细节丰富度
    location = random.choice(LOCATIONS)
    templates = [
        f"[{location}] 学生{student_id}：{description}",
        f"学生{student_id}在{location}{description}",
        f"关于{student_id}的报告：{location}，{description}",
    ]

    return {
        "concept_iri": f"http://ontoevolve/education#{concept_label}",
        "description": random.choice(templates),
        "severity": severity,
        "location": location,
        "student_id": student_id,
    }


def gen_event(student, student_id, timestamp):
    """生成一个完整的事件 JSON."""
    behavior = make_behavior_text(student, student_id)
    return {
        "description": behavior["description"],
        "studentId": behavior["student_id"],
        "location": behavior["location"],
        "severity": behavior["severity"],
    }


def gen_evaluation(student, elapsed_days=0):
    """生成与事件对应的反馈评价."""
    # 基于学生特征生成合理的反馈分数
    base_effectiveness = random.uniform(0.3, 0.9)
    base_cost = random.uniform(0.1, 0.7)
    base_satisfaction = random.uniform(0.2, 0.9)
    return {
        "effectiveness": round(base_effectiveness, 2),
        "cost": round(base_cost, 2),
        "satisfaction": round(base_satisfaction, 2),
    }


# ============================================================
# 输出
# ============================================================
def main():
    parser = argparse.ArgumentParser(description="OntoEvolve 测试事件生成器")
    parser.add_argument("--count", type=int, default=20, help="要生成的事件数量")
    parser.add_argument("--mat", default="../student-data/student-mat.csv", help="student-mat.csv 路径")
    parser.add_argument("--por", default="../student-data/student-por.csv", help="student-por.csv 路径")
    parser.add_argument("--api", action="store_true", help="输出 curl 命令而非 JSON")
    parser.add_argument("--include-feedback", action="store_true", help="同时生成反馈评价")
    parser.add_argument("--base-url", default="http://localhost:8080", help="API base URL")
    args = parser.parse_args()

    # 加载学生数据
    students = load_students(args.mat, args.por)
    if not students:
        import sys
        print("Warning: 未找到 UCI 学生数据文件，使用合成数据", file=sys.stderr)
        students = [
            {'school': 'GP', 'sex': 'M', 'age': 16, 'address': 'U', 'studytime': 2, 'failures': 0, 'absences': 4, 'health': 5, 'g1': '12', 'g2': '13', 'g3': '13'},
            {'school': 'MS', 'sex': 'F', 'age': 17, 'address': 'R', 'studytime': 1, 'failures': 3, 'absences': 15, 'health': 3, 'g1': '8', 'g2': '9', 'g3': '8'},
        ]

    base_time = datetime.now() - timedelta(days=args.count // 2)

    if args.api:
        # 输出 curl 命令
        print("#!/bin/bash")
        print("# OntoEvolve 测试事件 — 批量导入脚本")
        print()

        for i in range(args.count):
            student = random.choice(students)
            student_id = generate_student_id(student, i)
            ts = base_time + timedelta(hours=random.randint(0, args.count * 12))
            event = gen_event(student, student_id, ts)

            event_json = json.dumps(event, ensure_ascii=False)
            escaped = event_json.replace("'", "'\\''")
            print(f'curl -s -X POST {args.base_url}/api/education/event \\')
            print(f'  -H "Content-Type: application/json" \\')
            print(f"  -d '{escaped}'")
            print()

            if args.include_feedback:
                fb = gen_evaluation(student)
                fb_json = json.dumps(fb, ensure_ascii=False)
                fb_escaped = fb_json.replace("'", "'\\''")
                print(f'curl -s -X POST {args.base_url}/api/education/evaluation \\')
                print(f'  -H "Content-Type: application/json" \\')
                print(f"  -d '{fb_escaped}'")
                print()
    else:
        # 输出 JSON（每行一个事件）
        output = {"events": [], "evaluations": []}
        for i in range(args.count):
            student = random.choice(students)
            student_id = generate_student_id(student, i)
            ts = base_time + timedelta(hours=random.randint(0, args.count * 12))
            event = gen_event(student, student_id, ts)
            event["timestamp"] = ts.isoformat()
            output["events"].append(event)

            if args.include_feedback:
                fb = gen_evaluation(student)
                output["evaluations"].append(fb)

        print(json.dumps(output, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
