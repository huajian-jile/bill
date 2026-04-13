/** 与后端 BillIncomeExpenseUtil 一致：收/支含「中性」或单独为「/」的为中性 */
export function isNeutralIncomeExpense(ie) {
  const s = String(ie ?? '').trim()
  if (!s) return false
  if (s === '/' || s === '／') return true
  return s.includes('中性')
}

export function isIncomeIncomeExpense(ie) {
  return ie != null && String(ie).includes('收入')
}

export function isExpenseIncomeExpense(ie) {
  return ie != null && String(ie).includes('支出')
}
