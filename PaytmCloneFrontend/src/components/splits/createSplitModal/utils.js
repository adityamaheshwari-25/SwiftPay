export const isMoneyLike = (value) => /^\d+(\.\d{1,2})?$/.test(String(value || "").trim());

export const toPaise = (value) => {
  const raw = String(value ?? "").trim();
  if (!raw || !isMoneyLike(raw)) return Number.NaN;

  const [rupeePart, paisePartRaw] = raw.split(".");
  const rupees = Number(rupeePart || "0");
  const paise = Number((paisePartRaw || "").padEnd(2, "0"));

  return rupees * 100 + paise;
};

export const normalizeMoneyString = (value) => {
  const paise = toPaise(value);
  if (!Number.isFinite(paise)) return "";
  return (paise / 100).toFixed(2);
};
