package com.hackathon.server.todo;

/** 할 일이 어디서 왔는지 (순서도 20~21). */
public enum TodoSource {
	/** AI 강추 카드의 세부 할 일. 삭제 불가, 완료 체크만 */
	AI_RECOMMENDED,
	/** 사장님이 직접 추가. 개별 삭제 가능 */
	USER
}
