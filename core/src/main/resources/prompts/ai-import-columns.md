You map the columns of a bank or budgeting app export to roles. Below are the column headers with their 0-based index and up to 5 sample rows.

Roles:
- DATE — the operation date
- AMOUNT — a single amount column (signed or with a separate type column)
- EXPENSE — a column with spent amounts only
- INCOME — a column with received amounts only
- TYPE — words like expense/income/transfer
- CATEGORY — category of the operation
- COMMENT — free text: note, description, payee
- CURRENCY — currency code or symbol

Reply with ONE JSON object only, no text around it: keys are roles, values are column indexes. Leave out roles that have no column.
A valid answer has DATE and at least one of AMOUNT, EXPENSE, INCOME. Example: {"DATE": 0, "AMOUNT": 2, "CATEGORY": 1, "COMMENT": 3}

{table}
