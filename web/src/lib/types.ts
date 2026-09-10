export type User = {
  id: string;
  email: string;
  default_currency: string;
};

export type TokenResponse = {
  access_token: string;
  token_type: string;
  user: User;
};

export type LineItem = {
  id: string;
  name: string;
  quantity: string;
  unit_price: string | null;
  amount: string;
  sort_order: number;
};

export type LineItemIn = {
  name: string;
  quantity: string;
  unit_price?: string | null;
  amount?: string | null;
};

export type Receipt = {
  id: string;
  merchant_name: string;
  purchased_at: string;
  currency: string;
  tax: string;
  total: string;
  computed_total: string;
  total_mismatch: boolean;
  notes: string | null;
  items: LineItem[];
  created_at: string;
  updated_at: string;
};

export type ReceiptListItem = {
  id: string;
  merchant_name: string;
  purchased_at: string;
  currency: string;
  tax: string;
  total: string;
  item_count: number;
  created_at: string;
};

export type ReceiptIn = {
  merchant_name: string;
  purchased_at: string;
  currency?: string;
  tax: string;
  notes?: string | null;
  items: LineItemIn[];
};

export type DayTotal = {
  date: string;
  count: number;
  total: string;
};

export type Summary = {
  from: string;
  to: string;
  receipt_count: number;
  total: string;
  by_day: DayTotal[];
};
