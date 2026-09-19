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

export type Category = {
  id: string;
  name: string;
  is_default: boolean;
  sort_order: number;
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
  category: Category | null;
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
  category: Category | null;
  created_at: string;
};

export type ReceiptIn = {
  merchant_name: string;
  purchased_at: string;
  currency?: string;
  tax: string;
  notes?: string | null;
  category_id?: string | null;
  items: LineItemIn[];
};

export type DayTotal = {
  date: string;
  count: number;
  total: string;
};

export type CategoryTotal = {
  category_id: string | null;
  name: string;
  count: number;
  total: string;
};

export type Summary = {
  from: string;
  to: string;
  receipt_count: number;
  total: string;
  by_day: DayTotal[];
  by_category: CategoryTotal[];
};
