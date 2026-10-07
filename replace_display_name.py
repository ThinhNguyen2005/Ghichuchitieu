import os
import re

files_to_update = [
    "app/src/main/java/com/notepay/ui/component/TransactionItem.kt",
    "app/src/main/java/com/notepay/ui/component/DayDetailDialog.kt",
    "app/src/main/java/com/notepay/ui/feature/billsplit/BillSplitCreateSheet.kt",
    "app/src/main/java/com/notepay/ui/feature/billsplit/PaymentReconciliationSheet.kt",
    "app/src/main/java/com/notepay/ui/feature/category/CategoryManagementScreen.kt",
    "app/src/main/java/com/notepay/ui/feature/home/HomeScreen.kt",
    "app/src/main/java/com/notepay/ui/feature/stats/StatsScreen.kt",
    "app/src/main/java/com/notepay/ui/feature/subscription/AddSubscriptionBottomSheet.kt",
    "app/src/main/java/com/notepay/ui/feature/transaction/components/CategoryGridPicker.kt",
    "app/src/main/java/com/notepay/ui/feature/transaction/components/TransactionFormSections.kt",
    "app/src/main/java/com/notepay/ui/feature/transaction/components/TransactionInputComponents.kt",
    "app/src/main/java/com/notepay/ui/feature/transaction/detail/TransactionDetailScreen.kt",
    "app/src/main/java/com/notepay/ui/feature/transaction/edit/EditTransactionScreen.kt",
    "app/src/main/java/com/notepay/ui/feature/transaction/list/TransactionListScreen.kt"
]

import_statement = "import com.notepay.ui.util.localizedName\n"

for filepath in files_to_update:
    if not os.path.exists(filepath):
        continue
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # We only replace `.displayName` when it's accessed on an object, 
    # e.g. category.displayName, tx.category.displayName, etc.
    # Exclude cases where it's a parameter like `displayName: String`
    
    # Pattern to match: word.displayName -> word.localizedName()
    # It has to be preceded by a dot
    new_content = re.sub(r'\.displayName\b', '.localizedName()', content)
    
    if new_content != content:
        # Check if import is needed
        if "com.notepay.ui.util.localizedName" not in new_content:
            # Insert after the last import
            lines = new_content.split('\n')
            last_import_idx = -1
            for i, line in enumerate(lines):
                if line.startswith('import '):
                    last_import_idx = i
            
            if last_import_idx != -1:
                lines.insert(last_import_idx + 1, "import com.notepay.ui.util.localizedName")
                new_content = '\n'.join(lines)
            
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")
