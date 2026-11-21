// 원자재 가격 7단계. 4주 변동률(change4w)이 부류별 분포의 어디에 드는지로 정한다
export type PriceStage =
  | 'RISE_SEVERE'
  | 'RISE_ALERT'
  | 'RISE_CAUTION'
  | 'STABLE'
  | 'FALL_CAUTION'
  | 'FALL_ALERT'
  | 'FALL_SEVERE'

export type IngredientGroup =
  '채소류' | '수산물' | '식량작물' | '특용작물' | '과일류' | '식품' | '축산물'
