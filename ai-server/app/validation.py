"""LLM 출력 검사. 기준: docs/API_CONTRACT_DRAFT.md "검증과 실패", docs/PROMPT_DRAFT.md 검사 표.

칸을 빼야 하는 위반(길이·개수, 입력에 없는 숫자)과 기대 효과만 null로 바꾸면 되는 위반을 나눈다.
"""

import re
from dataclasses import dataclass, field

from app.schemas import LlmOutput, Suggestion

TITLE_MAX = 24
SUB_TASK_MAX = 24
SUB_TASKS_RANGE = (2, 3)
EFFECT_MAX = 60
EFFECT_ENDING = "수 있어요."
EFFECT_FORBIDDEN = ("매출", "수익", "방문객", "손님이 늘")

_NUMBER = re.compile(r"\d+(?:\.\d+)?")


def numbers(text: str) -> set[str]:
    """글 속 숫자. 천 단위 쉼표는 지우고 비교한다(54,200 = 54200)."""
    return set(_NUMBER.findall(text.replace(",", "")))


@dataclass
class Checked:
    kept: dict[str, Suggestion] = field(default_factory=dict)  # card_id → 남길 카드
    clean: set[str] = field(default_factory=set)  # 고칠 것 없이 통과한 card_id
    errors: list[str] = field(default_factory=list)  # 재요청 때 LLM에 알려 줄 위반


def _card_errors(s: Suggestion, allowed: set[str]) -> list[str]:
    errors = []
    if not s.title.strip() or len(s.title) > TITLE_MAX:
        errors.append(f"title은 {TITLE_MAX}자 이내여야 해요")
    low, high = SUB_TASKS_RANGE
    if not low <= len(s.sub_tasks) <= high:
        errors.append(f"sub_tasks는 {low}~{high}개여야 해요")
    if any(not t.strip() or len(t) > SUB_TASK_MAX for t in s.sub_tasks):
        errors.append(f"sub_tasks는 각 {SUB_TASK_MAX}자 이내여야 해요")
    unknown = numbers(" ".join([s.title, *s.sub_tasks])) - allowed
    if unknown:
        errors.append(f"입력에 없는 숫자를 썼어요: {', '.join(sorted(unknown))}")
    return errors


def _effect_errors(effect: str) -> list[str]:
    errors = []
    if len(effect) > EFFECT_MAX:
        errors.append(f"expected_effect는 {EFFECT_MAX}자 이내여야 해요")
    if numbers(effect):
        errors.append("expected_effect에 숫자를 쓰면 안 돼요")
    if any(word in effect for word in EFFECT_FORBIDDEN):
        errors.append("expected_effect에 매출·수익·방문객·손님 증가를 쓰면 안 돼요")
    if not effect.endswith(EFFECT_ENDING):
        errors.append(f'expected_effect는 "{EFFECT_ENDING}"로 끝나야 해요')
    return errors


def check(output: LlmOutput, card_ids: list[str], allowed_numbers: set[str]) -> Checked:
    result = Checked()
    expected = set(card_ids)
    seen: set[str] = set()
    for s in output.suggestions:
        if s.card_id not in expected:
            result.errors.append(f"요청에 없는 card_id {s.card_id}를 썼어요")
            continue
        if s.card_id in seen:
            result.errors.append(f"{s.card_id}를 두 번 썼어요")
            continue
        seen.add(s.card_id)
        card_errors = _card_errors(s, allowed_numbers)
        if card_errors:
            result.errors.extend(f"{s.card_id}: {e}" for e in card_errors)
            continue
        effect_errors = _effect_errors(s.expected_effect) if s.expected_effect is not None else []
        if effect_errors:
            result.errors.extend(f"{s.card_id}: {e}" for e in effect_errors)
            result.kept[s.card_id] = s.model_copy(update={"expected_effect": None})
        else:
            result.kept[s.card_id] = s
            result.clean.add(s.card_id)
    missing = [c for c in card_ids if c not in seen]
    if missing:
        result.errors.append(f"빠진 card_id가 있어요: {', '.join(missing)}")
    return result
