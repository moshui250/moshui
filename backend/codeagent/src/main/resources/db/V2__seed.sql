-- CodeAgent 智码刷题平台 · S2 种子数据
-- 先执行 V1__init.sql；本脚本可重复执行，按固定主键覆盖种子数据。
USE codeagent;
SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO knowledge_point (id, code, name, description, sort) VALUES
(1, 'array', '数组', '数组基础：遍历、子段和、环形数组。', 1),
(2, 'string', '字符串', '字符串：回文、滑动窗口、中心展开。', 2),
(3, 'linked_list', '链表', '链表：反转、回文、重排。', 3),
(4, 'recursion', '递归', '递归：最大值、最大公约数、分治。', 4),
(5, 'sort', '排序', '排序：升序、中位数、逆序对。', 5)
ON DUPLICATE KEY UPDATE code=VALUES(code), name=VALUES(name), description=VALUES(description), sort=VALUES(sort);

INSERT INTO question (id, title, description, input_desc, output_desc, difficulty, knowledge_point_id, template_json, type, user_id, source_image_url, create_time, update_time, deleted) VALUES
(1, '数组元素求和', '## 题目描述

给定 n 个整数，请计算它们的总和。

## 输入格式

第一行一个整数 n。第二行 n 个整数；当 n = 0 时第二行为空。

## 输出格式

输出一个整数，表示数组元素之和。

## 示例

### 示例 1

```text
输入
5
1 2 3 4 5
输出
15
```

### 示例 2

```text
输入
3
-1 0 1
输出
0
```

## 数据范围

0 ≤ n ≤ 100000，-10000 ≤ ai ≤ 10000。

## 提示

n = 0 时和为 0；答案可能超过 int 范围，请使用 64 位整数。', '第一行一个整数 n。第二行 n 个整数；当 n = 0 时第二行为空。', '输出一个整数，表示数组元素之和。', 1, 1, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(2, '多数元素', '## 题目描述

给定一个长度为 n 的数组，其中有一个元素出现的次数严格大于 n/2。请找出并输出这个元素。

## 输入格式

第一行一个整数 n。第二行 n 个整数。

## 输出格式

输出出现次数严格大于 n/2 的元素。

## 示例

### 示例 1

```text
输入
7
2 2 1 1 1 2 2
输出
2
```

### 示例 2

```text
输入
5
3 3 4 2 3
输出
3
```

## 数据范围

1 ≤ n ≤ 100000，-10^9 ≤ ai ≤ 10^9；保证存在出现次数严格大于 n/2 的元素。

## 提示

可以排序后取中间元素，也可以使用摩尔投票。题目保证答案一定存在。', '第一行一个整数 n。第二行 n 个整数。', '输出出现次数严格大于 n/2 的元素。', 2, 1, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(3, '环形子数组最大和', '## 题目描述

给定一个长度为 n 的整数数组，把数组看成首尾相接的环。请选择一个非空连续段，使元素之和最大，输出这个最大和。每个元素在所选段中最多使用一次。

## 输入格式

第一行一个整数 n。第二行 n 个整数。

## 输出格式

输出一个整数，表示环形数组的最大非空连续段和。

## 示例

### 示例 1

```text
输入
3
5 -3 5
输出
10
```

### 示例 2

```text
输入
5
1 -2 3 -2 1
输出
3
```

## 数据范围

1 ≤ n ≤ 100000，-10000 ≤ ai ≤ 10000。

## 提示

注意全是负数时不能选空段，此时答案就是最大的那个负数。', '第一行一个整数 n。第二行 n 个整数。', '输出一个整数，表示环形数组的最大非空连续段和。', 3, 1, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(4, '回文串判断', '## 题目描述

给定 n 个 0 到 25 的整数，依次表示一个只含小写字母的字符串。请判断这个字符串是否为回文串。

## 输入格式

第一行一个整数 n。第二行 n 个整数 x，0 ≤ x ≤ 25，x 表示字母表中第 x 个字母（0 表示 a，25 表示 z）。

## 输出格式

如果是回文串输出 YES，否则输出 NO。

## 示例

### 示例 1

```text
输入
5
0 1 2 1 0
输出
YES
```

### 示例 2

```text
输入
4
0 1 2 0
输出
NO
```

## 数据范围

1 ≤ n ≤ 100000，0 ≤ ai ≤ 25。

## 提示

空串和单个字符都可以视为回文；本题 n 至少为 1。', '第一行一个整数 n。第二行 n 个整数 x，0 ≤ x ≤ 25，x 表示字母表中第 x 个字母（0 表示 a，25 表示 z）。', '如果是回文串输出 YES，否则输出 NO。', 1, 2, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(5, '最长无重复子串长度', '## 题目描述

给定 n 个 0 到 25 的整数，依次表示一个只含小写字母的字符串。请找出其中不含重复字符的最长连续子串，输出它的长度。

## 输入格式

第一行一个整数 n。第二行 n 个整数 x，0 ≤ x ≤ 25，x 表示字母表中第 x 个字母。

## 输出格式

输出一个整数，表示最长无重复子串的长度。

## 示例

### 示例 1

```text
输入
8
0 1 2 0 1 2 1 1
输出
3
```

### 示例 2

```text
输入
5
0 1 0 2 1
输出
3
```

## 数据范围

1 ≤ n ≤ 100000，0 ≤ ai ≤ 25。

## 提示

窗口内始终维护「无重复字符」；遇到重复字符时，把左边界移到上次出现位置的后一位。', '第一行一个整数 n。第二行 n 个整数 x，0 ≤ x ≤ 25，x 表示字母表中第 x 个字母。', '输出一个整数，表示最长无重复子串的长度。', 2, 2, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(6, '最长回文子串长度', '## 题目描述

给定 n 个 0 到 25 的整数，依次表示一个只含小写字母的字符串。请输出这个字符串中最长回文子串的长度。

## 输入格式

第一行一个整数 n。第二行 n 个整数 x，0 ≤ x ≤ 25，x 表示字母表中第 x 个字母。

## 输出格式

输出一个整数，表示最长回文子串的长度。

## 示例

### 示例 1

```text
输入
5
1 0 1 0 3
输出
3
```

### 示例 2

```text
输入
4
2 1 1 3
输出
2
```

## 数据范围

1 ≤ n ≤ 100000，0 ≤ ai ≤ 25。

## 提示

中心既可能落在一个字符上，也可能落在两个字符之间，因此共有 2n-1 个中心。', '第一行一个整数 n。第二行 n 个整数 x，0 ≤ x ≤ 25，x 表示字母表中第 x 个字母。', '输出一个整数，表示最长回文子串的长度。', 3, 2, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(7, '单链表反转', '## 题目描述

给定 n 个整数，请按输入顺序构造一个单链表，将链表反转后按顺序输出所有结点的值。

## 输入格式

第一行一个整数 n。第二行 n 个整数，按顺序表示链表结点的值。

## 输出格式

按反转后的顺序输出 n 个整数，相邻整数用一个空格分隔。

## 示例

### 示例 1

```text
输入
5
1 2 3 4 5
输出
5 4 3 2 1
```

### 示例 2

```text
输入
4
10 20 30 40
输出
40 30 20 10
```

## 数据范围

1 ≤ n ≤ 100000，-10^9 ≤ 结点值 ≤ 10^9。

## 提示

反转后原来的尾结点变成新的头结点；注意先保存 next，再修改指针。', '第一行一个整数 n。第二行 n 个整数，按顺序表示链表结点的值。', '按反转后的顺序输出 n 个整数，相邻整数用一个空格分隔。', 1, 3, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

struct ListNode {
    int val;
    ListNode* next;
    ListNode(int x) : val(x), next(nullptr) {}
};

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    ListNode* head = nullptr;
    ListNode* tail = nullptr;
    for (int i = 0; i < n; ++i) {
        int x;
        cin >> x;
        ListNode* node = new ListNode(x);
        if (head == nullptr) {
            head = node;
        } else {
            tail->next = node;
        }
        tail = node;
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

class ListNode {
    int val;
    ListNode next;
    ListNode(int x) {
        val = x;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        ListNode head = null;
        ListNode tail = null;
        for (int i = 0; i < n; i++) {
            ListNode node = new ListNode(sc.nextInt());
            if (head == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(8, '回文链表', '## 题目描述

给定 n 个整数，请按输入顺序构造一个单链表，判断该链表是否为回文链表。

## 输入格式

第一行一个整数 n。第二行 n 个整数，按顺序表示链表结点的值。

## 输出格式

如果是回文链表输出 YES，否则输出 NO。

## 示例

### 示例 1

```text
输入
5
1 2 3 2 1
输出
YES
```

### 示例 2

```text
输入
4
1 2 2 3
输出
NO
```

## 数据范围

1 ≤ n ≤ 100000，-10^9 ≤ 结点值 ≤ 10^9。

## 提示

中点之前的结点保持原样；比较时只需要走到后半段结束。', '第一行一个整数 n。第二行 n 个整数，按顺序表示链表结点的值。', '如果是回文链表输出 YES，否则输出 NO。', 2, 3, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

struct ListNode {
    int val;
    ListNode* next;
    ListNode(int x) : val(x), next(nullptr) {}
};

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    ListNode* head = nullptr;
    ListNode* tail = nullptr;
    for (int i = 0; i < n; ++i) {
        int x;
        cin >> x;
        ListNode* node = new ListNode(x);
        if (head == nullptr) {
            head = node;
        } else {
            tail->next = node;
        }
        tail = node;
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

class ListNode {
    int val;
    ListNode next;
    ListNode(int x) {
        val = x;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        ListNode head = null;
        ListNode tail = null;
        for (int i = 0; i < n; i++) {
            ListNode node = new ListNode(sc.nextInt());
            if (head == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(9, '重排链表', '## 题目描述

给定 n 个整数，请按输入顺序构造一个单链表，并按 L0 → Ln → L1 → Ln-1 → ... 的顺序重排链表，最后输出重排后的结点值。

## 输入格式

第一行一个整数 n。第二行 n 个整数，按顺序表示链表结点的值。

## 输出格式

按重排后的顺序输出 n 个整数，相邻整数用一个空格分隔。

## 示例

### 示例 1

```text
输入
5
1 2 3 4 5
输出
1 5 2 4 3
```

### 示例 2

```text
输入
4
10 20 30 40
输出
10 40 20 30
```

## 数据范围

1 ≤ n ≤ 100000，-10^9 ≤ 结点值 ≤ 10^9。

## 提示

重排后的第一个结点仍然是原头结点；后半段反转后从原尾结点开始插入。', '第一行一个整数 n。第二行 n 个整数，按顺序表示链表结点的值。', '按重排后的顺序输出 n 个整数，相邻整数用一个空格分隔。', 3, 3, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

struct ListNode {
    int val;
    ListNode* next;
    ListNode(int x) : val(x), next(nullptr) {}
};

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    ListNode* head = nullptr;
    ListNode* tail = nullptr;
    for (int i = 0; i < n; ++i) {
        int x;
        cin >> x;
        ListNode* node = new ListNode(x);
        if (head == nullptr) {
            head = node;
        } else {
            tail->next = node;
        }
        tail = node;
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

class ListNode {
    int val;
    ListNode next;
    ListNode(int x) {
        val = x;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        ListNode head = null;
        ListNode tail = null;
        for (int i = 0; i < n; i++) {
            ListNode node = new ListNode(sc.nextInt());
            if (head == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(10, '递归求数组最大值', '## 题目描述

给定 n 个整数，请使用递归求数组中的最大值。

## 输入格式

第一行一个整数 n。第二行 n 个整数。

## 输出格式

输出数组中的最大值。

## 示例

### 示例 1

```text
输入
5
3 1 4 1 5
输出
5
```

### 示例 2

```text
输入
4
-3 -1 -4 -2
输出
-1
```

## 数据范围

1 ≤ n ≤ 1000，-10^9 ≤ ai ≤ 10^9。

## 提示

递归出口是最后一个元素；每一层返回当前元素与后缀最大值的较大者。', '第一行一个整数 n。第二行 n 个整数。', '输出数组中的最大值。', 1, 4, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(11, '递归求数组最大公约数', '## 题目描述

给定 n 个正整数，请使用递归求它们的最大公约数。

## 输入格式

第一行一个整数 n。第二行 n 个正整数。

## 输出格式

输出所有正整数的最大公约数。

## 示例

### 示例 1

```text
输入
4
12 18 24 36
输出
6
```

### 示例 2

```text
输入
3
7 14 21
输出
7
```

## 数据范围

1 ≤ n ≤ 1000，1 ≤ ai ≤ 10^9。

## 提示

最大公约数满足结合律，因此可以从第一个数开始逐个合并。', '第一行一个整数 n。第二行 n 个正整数。', '输出所有正整数的最大公约数。', 2, 4, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(12, '递归求最大子段和', '## 题目描述

给定 n 个整数，请使用分治递归求最大子段和，即所有非空连续子数组和的最大值。

## 输入格式

第一行一个整数 n。第二行 n 个整数。

## 输出格式

输出最大子段和。

## 示例

### 示例 1

```text
输入
8
-2 1 -3 4 -1 2 1 -5
输出
6
```

### 示例 2

```text
输入
5
1 -2 3 4 -1
输出
7
```

## 数据范围

1 ≤ n ≤ 100000，-10000 ≤ ai ≤ 10000。

## 提示

递归出口是区间只有一个元素；跨中点时左右两边都至少取一个元素。', '第一行一个整数 n。第二行 n 个整数。', '输出最大子段和。', 3, 4, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(13, '数组升序排序', '## 题目描述

给定 n 个整数，请将它们按升序排列后输出。

## 输入格式

第一行一个整数 n。第二行 n 个整数。

## 输出格式

按升序输出 n 个整数，相邻整数用一个空格分隔。

## 示例

### 示例 1

```text
输入
5
5 2 9 1 5
输出
1 2 5 5 9
```

### 示例 2

```text
输入
4
-1 3 0 -2
输出
-2 -1 0 3
```

## 数据范围

1 ≤ n ≤ 100000，-10^9 ≤ ai ≤ 10^9。

## 提示

输出时相邻整数之间恰好一个空格，行末不要多余空格。', '第一行一个整数 n。第二行 n 个整数。', '按升序输出 n 个整数，相邻整数用一个空格分隔。', 1, 5, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(14, '数组中位数', '## 题目描述

给定 n 个整数，请输出排序后的中位数。当 n 为偶数时，取下标 (n-1)/2 的较小中位数。

## 输入格式

第一行一个整数 n。第二行 n 个整数。

## 输出格式

输出一个整数，表示中位数。

## 示例

### 示例 1

```text
输入
5
3 1 4 1 5
输出
3
```

### 示例 2

```text
输入
6
7 1 5 3 9 2
输出
3
```

## 数据范围

1 ≤ n ≤ 100000，-10^9 ≤ ai ≤ 10^9。

## 提示

奇数个元素时 (n-1)/2 正好是正中间；偶数个元素时取靠左的那个。', '第一行一个整数 n。第二行 n 个整数。', '输出一个整数，表示中位数。', 2, 5, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0),
(15, '数组逆序对数量', '## 题目描述

给定 n 个整数，请统计数组中的逆序对数量。逆序对指满足 i < j 且 a[i] > a[j] 的下标对。

## 输入格式

第一行一个整数 n。第二行 n 个整数。

## 输出格式

输出一个整数，表示逆序对数量。

## 示例

### 示例 1

```text
输入
5
2 3 1 5 4
输出
3
```

### 示例 2

```text
输入
4
4 3 2 1
输出
6
```

## 数据范围

1 ≤ n ≤ 100000，-10^9 ≤ ai ≤ 10^9。

## 提示

逆序对数量可能达到 n(n-1)/2，需要使用 64 位整数保存答案。', '第一行一个整数 n。第二行 n 个整数。', '输出一个整数，表示逆序对数量。', 3, 5, JSON_OBJECT('cpp', '#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }

    // 在此编写你的代码

    return 0;
}', 'java', 'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        // 在此编写你的代码
    }
}'), 'PUBLIC', NULL, NULL, '2026-11-02 22:00:00', '2026-11-02 22:00:00', 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description), input_desc=VALUES(input_desc), output_desc=VALUES(output_desc), difficulty=VALUES(difficulty), knowledge_point_id=VALUES(knowledge_point_id), template_json=VALUES(template_json), type=VALUES(type), user_id=VALUES(user_id), source_image_url=VALUES(source_image_url), update_time=VALUES(update_time), deleted=VALUES(deleted);

INSERT INTO question_test_case (id, question_id, input_data, expected_output, is_sample, score, sort_no) VALUES
(1, 1, '5
1 2 3 4 5', '15', 1, 25, 1),
(2, 1, '3
-1 0 1', '0', 1, 25, 2),
(3, 1, '0
', '0', 0, 25, 3),
(4, 1, '6
10000 10000 10000 10000 10000 10000', '60000', 0, 25, 4),
(5, 2, '7
2 2 1 1 1 2 2', '2', 1, 25, 1),
(6, 2, '5
3 3 4 2 3', '3', 1, 25, 2),
(7, 2, '1
42', '42', 0, 25, 3),
(8, 2, '6
7 7 7 7 7 7', '7', 0, 25, 4),
(9, 3, '3
5 -3 5', '10', 1, 25, 1),
(10, 3, '5
1 -2 3 -2 1', '3', 1, 25, 2),
(11, 3, '1
-7', '-7', 0, 25, 3),
(12, 3, '5
-2 -3 -1 -4 -5', '-1', 0, 25, 4),
(13, 4, '5
0 1 2 1 0', 'YES', 1, 25, 1),
(14, 4, '4
0 1 2 0', 'NO', 1, 25, 2),
(15, 4, '1
25', 'YES', 0, 25, 3),
(16, 4, '6
3 3 3 3 3 3', 'YES', 0, 25, 4),
(17, 5, '8
0 1 2 0 1 2 1 1', '3', 1, 25, 1),
(18, 5, '5
0 1 0 2 1', '3', 1, 25, 2),
(19, 5, '1
7', '1', 0, 25, 3),
(20, 5, '6
4 4 4 4 4 4', '1', 0, 25, 4),
(21, 6, '5
1 0 1 0 3', '3', 1, 25, 1),
(22, 6, '4
2 1 1 3', '2', 1, 25, 2),
(23, 6, '1
9', '1', 0, 25, 3),
(24, 6, '6
2 2 2 2 2 2', '6', 0, 25, 4),
(25, 7, '5
1 2 3 4 5', '5 4 3 2 1', 1, 25, 1),
(26, 7, '4
10 20 30 40', '40 30 20 10', 1, 25, 2),
(27, 7, '1
7', '7', 0, 25, 3),
(28, 7, '6
3 3 3 3 3 3', '3 3 3 3 3 3', 0, 25, 4),
(29, 8, '5
1 2 3 2 1', 'YES', 1, 25, 1),
(30, 8, '4
1 2 2 3', 'NO', 1, 25, 2),
(31, 8, '1
5', 'YES', 0, 25, 3),
(32, 8, '6
8 8 8 8 8 8', 'YES', 0, 25, 4),
(33, 9, '5
1 2 3 4 5', '1 5 2 4 3', 1, 25, 1),
(34, 9, '4
10 20 30 40', '10 40 20 30', 1, 25, 2),
(35, 9, '1
7', '7', 0, 25, 3),
(36, 9, '6
2 2 2 2 2 2', '2 2 2 2 2 2', 0, 25, 4),
(37, 10, '5
3 1 4 1 5', '5', 1, 25, 1),
(38, 10, '4
-3 -1 -4 -2', '-1', 1, 25, 2),
(39, 10, '1
-9', '-9', 0, 25, 3),
(40, 10, '5
6 6 6 6 6', '6', 0, 25, 4),
(41, 11, '4
12 18 24 36', '6', 1, 25, 1),
(42, 11, '3
7 14 21', '7', 1, 25, 2),
(43, 11, '1
13', '13', 0, 25, 3),
(44, 11, '5
9 9 9 9 9', '9', 0, 25, 4),
(45, 12, '8
-2 1 -3 4 -1 2 1 -5', '6', 1, 25, 1),
(46, 12, '5
1 -2 3 4 -1', '7', 1, 25, 2),
(47, 12, '1
-4', '-4', 0, 25, 3),
(48, 12, '6
2 2 2 2 2 2', '12', 0, 25, 4),
(49, 13, '5
5 2 9 1 5', '1 2 5 5 9', 1, 25, 1),
(50, 13, '4
-1 3 0 -2', '-2 -1 0 3', 1, 25, 2),
(51, 13, '1
8', '8', 0, 25, 3),
(52, 13, '6
4 4 4 4 4 4', '4 4 4 4 4 4', 0, 25, 4),
(53, 14, '5
3 1 4 1 5', '3', 1, 25, 1),
(54, 14, '6
7 1 5 3 9 2', '3', 1, 25, 2),
(55, 14, '1
42', '42', 0, 25, 3),
(56, 14, '5
6 6 6 6 6', '6', 0, 25, 4),
(57, 15, '5
2 3 1 5 4', '3', 1, 25, 1),
(58, 15, '4
4 3 2 1', '6', 1, 25, 2),
(59, 15, '1
7', '0', 0, 25, 3),
(60, 15, '6
5 5 5 5 5 5', '0', 0, 25, 4)
ON DUPLICATE KEY UPDATE question_id=VALUES(question_id), input_data=VALUES(input_data), expected_output=VALUES(expected_output), is_sample=VALUES(is_sample), score=VALUES(score), sort_no=VALUES(sort_no);

INSERT INTO question_solution (question_id, language, content, explanation) VALUES
(1, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 2 3 4 5 -> 15
// 用例2: 3 / -1 0 1 -> 0
// 用例3: 0 -> 0
// 用例4: 6 / 10000 10000 10000 10000 10000 10000 -> 60000
#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    long long sum = 0;
    for (int i = 0; i < n; ++i) {
        long long x;
        cin >> x;
        sum += x;
    }
    cout << sum << endl;
    return 0;
}', '遍历数组累加即可；n = 0 时自然得到 0。时间复杂度 O(n)，空间复杂度 O(1)。'),
(1, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 2 3 4 5 -> 15
// 用例2: 3 / -1 0 1 -> 0
// 用例3: 0 -> 0
// 用例4: 6 / 10000 10000 10000 10000 10000 10000 -> 60000
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += sc.nextLong();
        }
        System.out.println(sum);
    }
}', '遍历数组累加即可；n = 0 时自然得到 0。时间复杂度 O(n)，空间复杂度 O(1)。'),
(2, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 7 / 2 2 1 1 1 2 2 -> 2
// 用例2: 5 / 3 3 4 2 3 -> 3
// 用例3: 1 / 42 -> 42
// 用例4: 6 / 7 7 7 7 7 7 -> 7
#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    long long candidate = 0;
    int count = 0;
    for (int i = 0; i < n; ++i) {
        long long x;
        cin >> x;
        if (count == 0) {
            candidate = x;
            count = 1;
        } else if (x == candidate) {
            ++count;
        } else {
            --count;
        }
    }
    cout << candidate << endl;
    return 0;
}', '摩尔投票：维护候选值和计数，相同加一，不同减一，最后剩下的候选值就是答案。时间复杂度 O(n)，空间复杂度 O(1)。'),
(2, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 7 / 2 2 1 1 1 2 2 -> 2
// 用例2: 5 / 3 3 4 2 3 -> 3
// 用例3: 1 / 42 -> 42
// 用例4: 6 / 7 7 7 7 7 7 -> 7
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long candidate = 0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            long x = sc.nextLong();
            if (count == 0) {
                candidate = x;
                count = 1;
            } else if (x == candidate) {
                count++;
            } else {
                count--;
            }
        }
        System.out.println(candidate);
    }
}', '摩尔投票：维护候选值和计数，相同加一，不同减一，最后剩下的候选值就是答案。时间复杂度 O(n)，空间复杂度 O(1)。'),
(3, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 3 / 5 -3 5 -> 10
// 用例2: 5 / 1 -2 3 -2 1 -> 3
// 用例3: 1 / -7 -> -7
// 用例4: 5 / -2 -3 -1 -4 -5 -> -1
#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    long long total = 0;
    long long currentMax = 0;
    long long bestMax = LLONG_MIN;
    long long currentMin = 0;
    long long bestMin = LLONG_MAX;
    for (int i = 0; i < n; ++i) {
        long long x;
        cin >> x;
        total += x;
        currentMax = max(currentMax + x, x);
        bestMax = max(bestMax, currentMax);
        currentMin = min(currentMin + x, x);
        bestMin = min(bestMin, currentMin);
    }
    long long answer = bestMax;
    if (bestMax >= 0) {
        answer = max(bestMax, total - bestMin);
    }
    cout << answer << endl;
    return 0;
}', '分别求普通最大子段和与最小子段和。若最大子段和小于 0，说明全为负数，答案为最大元素；否则答案为 max(普通最大子段和, 总和 - 最小子段和)。时间复杂度 O(n)。'),
(3, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 3 / 5 -3 5 -> 10
// 用例2: 5 / 1 -2 3 -2 1 -> 3
// 用例3: 1 / -7 -> -7
// 用例4: 5 / -2 -3 -1 -4 -5 -> -1
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long total = 0;
        long currentMax = 0;
        long bestMax = Long.MIN_VALUE;
        long currentMin = 0;
        long bestMin = Long.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            long x = sc.nextLong();
            total += x;
            currentMax = Math.max(currentMax + x, x);
            bestMax = Math.max(bestMax, currentMax);
            currentMin = Math.min(currentMin + x, x);
            bestMin = Math.min(bestMin, currentMin);
        }
        long answer = bestMax;
        if (bestMax >= 0) {
            answer = Math.max(bestMax, total - bestMin);
        }
        System.out.println(answer);
    }
}', '分别求普通最大子段和与最小子段和。若最大子段和小于 0，说明全为负数，答案为最大元素；否则答案为 max(普通最大子段和, 总和 - 最小子段和)。时间复杂度 O(n)。'),
(4, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 0 1 2 1 0 -> YES
// 用例2: 4 / 0 1 2 0 -> NO
// 用例3: 1 / 25 -> YES
// 用例4: 6 / 3 3 3 3 3 3 -> YES
#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<int> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }
    bool ok = true;
    for (int i = 0; i < n / 2; ++i) {
        if (a[i] != a[n - 1 - i]) {
            ok = false;
            break;
        }
    }
    cout << (ok ? "YES" : "NO") << endl;
    return 0;
}', '双指针从两端向中间比较，全部相等即为回文。时间复杂度 O(n)，空间复杂度 O(1)。'),
(4, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 0 1 2 1 0 -> YES
// 用例2: 4 / 0 1 2 0 -> NO
// 用例3: 1 / 25 -> YES
// 用例4: 6 / 3 3 3 3 3 3 -> YES
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextInt();
        }
        boolean ok = true;
        for (int i = 0; i < n / 2; i++) {
            if (a[i] != a[n - 1 - i]) {
                ok = false;
                break;
            }
        }
        System.out.println(ok ? "YES" : "NO");
    }
}', '双指针从两端向中间比较，全部相等即为回文。时间复杂度 O(n)，空间复杂度 O(1)。'),
(5, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 8 / 0 1 2 0 1 2 1 1 -> 3
// 用例2: 5 / 0 1 0 2 1 -> 3
// 用例3: 1 / 7 -> 1
// 用例4: 6 / 4 4 4 4 4 4 -> 1
#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<int> last(26, -1);
    int left = 0;
    int answer = 0;
    for (int i = 0; i < n; ++i) {
        int x;
        cin >> x;
        if (last[x] >= left) {
            left = last[x] + 1;
        }
        answer = max(answer, i - left + 1);
        last[x] = i;
    }
    cout << answer << endl;
    return 0;
}', '滑动窗口配合每个字符最后出现的位置，左边界只向右移动。时间复杂度 O(n)，空间复杂度 O(1)（字符集大小为 26）。'),
(5, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 8 / 0 1 2 0 1 2 1 1 -> 3
// 用例2: 5 / 0 1 0 2 1 -> 3
// 用例3: 1 / 7 -> 1
// 用例4: 6 / 4 4 4 4 4 4 -> 1
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] last = new int[26];
        Arrays.fill(last, -1);
        int left = 0;
        int answer = 0;
        for (int i = 0; i < n; i++) {
            int x = sc.nextInt();
            if (last[x] >= left) {
                left = last[x] + 1;
            }
            answer = Math.max(answer, i - left + 1);
            last[x] = i;
        }
        System.out.println(answer);
    }
}', '滑动窗口配合每个字符最后出现的位置，左边界只向右移动。时间复杂度 O(n)，空间复杂度 O(1)（字符集大小为 26）。'),
(6, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 0 1 0 3 -> 3
// 用例2: 4 / 2 1 1 3 -> 2
// 用例3: 1 / 9 -> 1
// 用例4: 6 / 2 2 2 2 2 2 -> 6
#include <bits/stdc++.h>
using namespace std;

int expand(const vector<int>& a, int left, int right) {
    while (left >= 0 && right < (int)a.size() && a[left] == a[right]) {
        --left;
        ++right;
    }
    return right - left - 1;
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<int> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }
    int answer = 1;
    for (int center = 0; center < 2 * n - 1; ++center) {
        int left = center / 2;
        int right = left + center % 2;
        answer = max(answer, expand(a, left, right));
    }
    cout << answer << endl;
    return 0;
}', '枚举每个中心（奇数长度和偶数长度），向两边扩展，取最大半径。时间复杂度 O(n^2)，空间复杂度 O(1)。'),
(6, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 0 1 0 3 -> 3
// 用例2: 4 / 2 1 1 3 -> 2
// 用例3: 1 / 9 -> 1
// 用例4: 6 / 2 2 2 2 2 2 -> 6
import java.util.*;

public class Main {
    static int expand(int[] a, int left, int right) {
        while (left >= 0 && right < a.length && a[left] == a[right]) {
            left--;
            right++;
        }
        return right - left - 1;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextInt();
        }
        int answer = 1;
        for (int center = 0; center < 2 * n - 1; center++) {
            int left = center / 2;
            int right = left + center % 2;
            answer = Math.max(answer, expand(a, left, right));
        }
        System.out.println(answer);
    }
}', '枚举每个中心（奇数长度和偶数长度），向两边扩展，取最大半径。时间复杂度 O(n^2)，空间复杂度 O(1)。'),
(7, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 2 3 4 5 -> 5 4 3 2 1
// 用例2: 4 / 10 20 30 40 -> 40 30 20 10
// 用例3: 1 / 7 -> 7
// 用例4: 6 / 3 3 3 3 3 3 -> 3 3 3 3 3 3
#include <bits/stdc++.h>
using namespace std;

struct ListNode {
    int val;
    ListNode* next;
    ListNode(int x) : val(x), next(nullptr) {}
};

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    ListNode* head = nullptr;
    ListNode* tail = nullptr;
    for (int i = 0; i < n; ++i) {
        int x;
        cin >> x;
        ListNode* node = new ListNode(x);
        if (head == nullptr) {
            head = node;
        } else {
            tail->next = node;
        }
        tail = node;
    }

    ListNode* previous = nullptr;
    ListNode* current = head;
    while (current != nullptr) {
        ListNode* nextNode = current->next;
        current->next = previous;
        previous = current;
        current = nextNode;
    }
    head = previous;

    bool first = true;
    for (ListNode* node = head; node != nullptr; node = node->next) {
        if (!first) {
            cout << " ";
        }
        cout << node->val;
        first = false;
    }
    cout << endl;
    return 0;
}', '三指针遍历链表，依次把当前结点的 next 指向它的前驱。时间复杂度 O(n)，空间复杂度 O(1)。'),
(7, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 2 3 4 5 -> 5 4 3 2 1
// 用例2: 4 / 10 20 30 40 -> 40 30 20 10
// 用例3: 1 / 7 -> 7
// 用例4: 6 / 3 3 3 3 3 3 -> 3 3 3 3 3 3
import java.util.*;

class ListNode {
    int val;
    ListNode next;
    ListNode(int x) {
        val = x;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        ListNode head = null;
        ListNode tail = null;
        for (int i = 0; i < n; i++) {
            ListNode node = new ListNode(sc.nextInt());
            if (head == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        ListNode previous = null;
        ListNode current = head;
        while (current != null) {
            ListNode nextNode = current.next;
            current.next = previous;
            previous = current;
            current = nextNode;
        }
        head = previous;

        StringBuilder sb = new StringBuilder();
        for (ListNode node = head; node != null; node = node.next) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(node.val);
        }
        System.out.println(sb.toString());
    }
}', '三指针遍历链表，依次把当前结点的 next 指向它的前驱。时间复杂度 O(n)，空间复杂度 O(1)。'),
(8, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 2 3 2 1 -> YES
// 用例2: 4 / 1 2 2 3 -> NO
// 用例3: 1 / 5 -> YES
// 用例4: 6 / 8 8 8 8 8 8 -> YES
#include <bits/stdc++.h>
using namespace std;

struct ListNode {
    int val;
    ListNode* next;
    ListNode(int x) : val(x), next(nullptr) {}
};

ListNode* reverseList(ListNode* head) {
    ListNode* previous = nullptr;
    while (head != nullptr) {
        ListNode* nextNode = head->next;
        head->next = previous;
        previous = head;
        head = nextNode;
    }
    return previous;
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    ListNode* head = nullptr;
    ListNode* tail = nullptr;
    for (int i = 0; i < n; ++i) {
        int x;
        cin >> x;
        ListNode* node = new ListNode(x);
        if (head == nullptr) {
            head = node;
        } else {
            tail->next = node;
        }
        tail = node;
    }

    ListNode* slow = head;
    ListNode* fast = head;
    while (fast->next != nullptr && fast->next->next != nullptr) {
        slow = slow->next;
        fast = fast->next->next;
    }

    ListNode* second = reverseList(slow->next);
    ListNode* first = head;
    bool ok = true;
    while (second != nullptr) {
        if (first->val != second->val) {
            ok = false;
            break;
        }
        first = first->next;
        second = second->next;
    }
    cout << (ok ? "YES" : "NO") << endl;
    return 0;
}', '快慢指针找到中点，反转后半段，再与前半段逐结点比较。时间复杂度 O(n)，空间复杂度 O(1)。'),
(8, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 2 3 2 1 -> YES
// 用例2: 4 / 1 2 2 3 -> NO
// 用例3: 1 / 5 -> YES
// 用例4: 6 / 8 8 8 8 8 8 -> YES
import java.util.*;

class ListNode {
    int val;
    ListNode next;
    ListNode(int x) {
        val = x;
    }
}

public class Main {
    static ListNode reverseList(ListNode head) {
        ListNode previous = null;
        while (head != null) {
            ListNode nextNode = head.next;
            head.next = previous;
            previous = head;
            head = nextNode;
        }
        return previous;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        ListNode head = null;
        ListNode tail = null;
        for (int i = 0; i < n; i++) {
            ListNode node = new ListNode(sc.nextInt());
            if (head == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        ListNode slow = head;
        ListNode fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }

        ListNode second = reverseList(slow.next);
        ListNode first = head;
        boolean ok = true;
        while (second != null) {
            if (first.val != second.val) {
                ok = false;
                break;
            }
            first = first.next;
            second = second.next;
        }
        System.out.println(ok ? "YES" : "NO");
    }
}', '快慢指针找到中点，反转后半段，再与前半段逐结点比较。时间复杂度 O(n)，空间复杂度 O(1)。'),
(9, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 2 3 4 5 -> 1 5 2 4 3
// 用例2: 4 / 10 20 30 40 -> 10 40 20 30
// 用例3: 1 / 7 -> 7
// 用例4: 6 / 2 2 2 2 2 2 -> 2 2 2 2 2 2
#include <bits/stdc++.h>
using namespace std;

struct ListNode {
    int val;
    ListNode* next;
    ListNode(int x) : val(x), next(nullptr) {}
};

ListNode* reverseList(ListNode* head) {
    ListNode* previous = nullptr;
    while (head != nullptr) {
        ListNode* nextNode = head->next;
        head->next = previous;
        previous = head;
        head = nextNode;
    }
    return previous;
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    ListNode* head = nullptr;
    ListNode* tail = nullptr;
    for (int i = 0; i < n; ++i) {
        int x;
        cin >> x;
        ListNode* node = new ListNode(x);
        if (head == nullptr) {
            head = node;
        } else {
            tail->next = node;
        }
        tail = node;
    }

    ListNode* slow = head;
    ListNode* fast = head;
    while (fast->next != nullptr && fast->next->next != nullptr) {
        slow = slow->next;
        fast = fast->next->next;
    }

    ListNode* second = reverseList(slow->next);
    slow->next = nullptr;
    ListNode* first = head;
    while (second != nullptr) {
        ListNode* firstNext = first->next;
        ListNode* secondNext = second->next;
        first->next = second;
        second->next = firstNext;
        first = firstNext;
        second = secondNext;
    }

    bool firstOutput = true;
    for (ListNode* node = head; node != nullptr; node = node->next) {
        if (!firstOutput) {
            cout << " ";
        }
        cout << node->val;
        firstOutput = false;
    }
    cout << endl;
    return 0;
}', '快慢指针找中点，反转后半段，再把两段交替合并。时间复杂度 O(n)，空间复杂度 O(1)。'),
(9, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 1 2 3 4 5 -> 1 5 2 4 3
// 用例2: 4 / 10 20 30 40 -> 10 40 20 30
// 用例3: 1 / 7 -> 7
// 用例4: 6 / 2 2 2 2 2 2 -> 2 2 2 2 2 2
import java.util.*;

class ListNode {
    int val;
    ListNode next;
    ListNode(int x) {
        val = x;
    }
}

public class Main {
    static ListNode reverseList(ListNode head) {
        ListNode previous = null;
        while (head != null) {
            ListNode nextNode = head.next;
            head.next = previous;
            previous = head;
            head = nextNode;
        }
        return previous;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        ListNode head = null;
        ListNode tail = null;
        for (int i = 0; i < n; i++) {
            ListNode node = new ListNode(sc.nextInt());
            if (head == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        ListNode slow = head;
        ListNode fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }

        ListNode second = reverseList(slow.next);
        slow.next = null;
        ListNode first = head;
        while (second != null) {
            ListNode firstNext = first.next;
            ListNode secondNext = second.next;
            first.next = second;
            second.next = firstNext;
            first = firstNext;
            second = secondNext;
        }

        StringBuilder sb = new StringBuilder();
        for (ListNode node = head; node != null; node = node.next) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(node.val);
        }
        System.out.println(sb.toString());
    }
}', '快慢指针找中点，反转后半段，再把两段交替合并。时间复杂度 O(n)，空间复杂度 O(1)。'),
(10, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 3 1 4 1 5 -> 5
// 用例2: 4 / -3 -1 -4 -2 -> -1
// 用例3: 1 / -9 -> -9
// 用例4: 5 / 6 6 6 6 6 -> 6
#include <bits/stdc++.h>
using namespace std;

int n;
vector<long long> a;

long long solve(int index) {
    if (index == n - 1) {
        return a[index];
    }
    return max(a[index], solve(index + 1));
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    cin >> n;
    a.resize(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }
    cout << solve(0) << endl;
    return 0;
}', '递归比较当前元素和剩余后缀的最大值。时间复杂度 O(n)，递归深度 O(n)。'),
(10, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 3 1 4 1 5 -> 5
// 用例2: 4 / -3 -1 -4 -2 -> -1
// 用例3: 1 / -9 -> -9
// 用例4: 5 / 6 6 6 6 6 -> 6
import java.util.*;

public class Main {
    static long[] a;

    static long solve(int index) {
        if (index == a.length - 1) {
            return a[index];
        }
        return Math.max(a[index], solve(index + 1));
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }
        System.out.println(solve(0));
    }
}', '递归比较当前元素和剩余后缀的最大值。时间复杂度 O(n)，递归深度 O(n)。'),
(11, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 4 / 12 18 24 36 -> 6
// 用例2: 3 / 7 14 21 -> 7
// 用例3: 1 / 13 -> 13
// 用例4: 5 / 9 9 9 9 9 -> 9
#include <bits/stdc++.h>
using namespace std;

long long gcd(long long a, long long b) {
    if (b == 0) {
        return a;
    }
    return gcd(b, a % b);
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    long long answer;
    cin >> answer;
    for (int i = 1; i < n; ++i) {
        long long x;
        cin >> x;
        answer = gcd(answer, x);
    }
    cout << answer << endl;
    return 0;
}', '辗转相除法递归求两个数的最大公约数，再与下一个数继续求。时间复杂度 O(n log V)，递归深度 O(log V)。'),
(11, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 4 / 12 18 24 36 -> 6
// 用例2: 3 / 7 14 21 -> 7
// 用例3: 1 / 13 -> 13
// 用例4: 5 / 9 9 9 9 9 -> 9
import java.util.*;

public class Main {
    static long gcd(long a, long b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long answer = sc.nextLong();
        for (int i = 1; i < n; i++) {
            answer = gcd(answer, sc.nextLong());
        }
        System.out.println(answer);
    }
}', '辗转相除法递归求两个数的最大公约数，再与下一个数继续求。时间复杂度 O(n log V)，递归深度 O(log V)。'),
(12, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 8 / -2 1 -3 4 -1 2 1 -5 -> 6
// 用例2: 5 / 1 -2 3 4 -1 -> 7
// 用例3: 1 / -4 -> -4
// 用例4: 6 / 2 2 2 2 2 2 -> 12
#include <bits/stdc++.h>
using namespace std;

int n;
vector<long long> a;

long long solve(int left, int right) {
    if (left == right) {
        return a[left];
    }
    int middle = (left + right) / 2;
    long long bestLeftPart = solve(left, middle);
    long long bestRightPart = solve(middle + 1, right);

    long long sum = 0;
    long long bestSuffix = LLONG_MIN;
    for (int i = middle; i >= left; --i) {
        sum += a[i];
        bestSuffix = max(bestSuffix, sum);
    }

    sum = 0;
    long long bestPrefix = LLONG_MIN;
    for (int i = middle + 1; i <= right; ++i) {
        sum += a[i];
        bestPrefix = max(bestPrefix, sum);
    }

    long long bestCross = bestSuffix + bestPrefix;
    return max({bestLeftPart, bestRightPart, bestCross});
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    cin >> n;
    a.resize(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }
    cout << solve(0, n - 1) << endl;
    return 0;
}', '分治：最大子段和要么完全在左半，要么完全在右半，要么跨过中点；跨中点的情况用向左的最大后缀和向右的最大前缀拼起来。时间复杂度 O(n log n)。'),
(12, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 8 / -2 1 -3 4 -1 2 1 -5 -> 6
// 用例2: 5 / 1 -2 3 4 -1 -> 7
// 用例3: 1 / -4 -> -4
// 用例4: 6 / 2 2 2 2 2 2 -> 12
import java.util.*;

public class Main {
    static long[] a;

    static long solve(int left, int right) {
        if (left == right) {
            return a[left];
        }
        int middle = (left + right) / 2;
        long bestLeftPart = solve(left, middle);
        long bestRightPart = solve(middle + 1, right);

        long sum = 0;
        long bestSuffix = Long.MIN_VALUE;
        for (int i = middle; i >= left; i--) {
            sum += a[i];
            bestSuffix = Math.max(bestSuffix, sum);
        }

        sum = 0;
        long bestPrefix = Long.MIN_VALUE;
        for (int i = middle + 1; i <= right; i++) {
            sum += a[i];
            bestPrefix = Math.max(bestPrefix, sum);
        }

        long bestCross = bestSuffix + bestPrefix;
        return Math.max(Math.max(bestLeftPart, bestRightPart), bestCross);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }
        System.out.println(solve(0, n - 1));
    }
}', '分治：最大子段和要么完全在左半，要么完全在右半，要么跨过中点；跨中点的情况用向左的最大后缀和向右的最大前缀拼起来。时间复杂度 O(n log n)。'),
(13, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 5 2 9 1 5 -> 1 2 5 5 9
// 用例2: 4 / -1 3 0 -2 -> -2 -1 0 3
// 用例3: 1 / 8 -> 8
// 用例4: 6 / 4 4 4 4 4 4 -> 4 4 4 4 4 4
#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }
    sort(a.begin(), a.end());
    for (int i = 0; i < n; ++i) {
        if (i > 0) {
            cout << " ";
        }
        cout << a[i];
    }
    cout << endl;
    return 0;
}', '直接使用标准库排序。时间复杂度 O(n log n)，空间复杂度取决于排序实现。'),
(13, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 5 2 9 1 5 -> 1 2 5 5 9
// 用例2: 4 / -1 3 0 -2 -> -2 -1 0 3
// 用例3: 1 / 8 -> 8
// 用例4: 6 / 4 4 4 4 4 4 -> 4 4 4 4 4 4
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }
        Arrays.sort(a);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(a[i]);
        }
        System.out.println(sb.toString());
    }
}', '直接使用标准库排序。时间复杂度 O(n log n)，空间复杂度取决于排序实现。'),
(14, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 3 1 4 1 5 -> 3
// 用例2: 6 / 7 1 5 3 9 2 -> 3
// 用例3: 1 / 42 -> 42
// 用例4: 5 / 6 6 6 6 6 -> 6
#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    vector<long long> a(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }
    sort(a.begin(), a.end());
    cout << a[(n - 1) / 2] << endl;
    return 0;
}', '排序后取下标 (n-1)/2 的元素；偶数个元素时它就是较小的中位数。时间复杂度 O(n log n)。'),
(14, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 3 1 4 1 5 -> 3
// 用例2: 6 / 7 1 5 3 9 2 -> 3
// 用例3: 1 / 42 -> 42
// 用例4: 5 / 6 6 6 6 6 -> 6
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }
        Arrays.sort(a);
        System.out.println(a[(n - 1) / 2]);
    }
}', '排序后取下标 (n-1)/2 的元素；偶数个元素时它就是较小的中位数。时间复杂度 O(n log n)。'),
(15, 'CPP', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 2 3 1 5 4 -> 3
// 用例2: 4 / 4 3 2 1 -> 6
// 用例3: 1 / 7 -> 0
// 用例4: 6 / 5 5 5 5 5 5 -> 0
#include <bits/stdc++.h>
using namespace std;

int n;
vector<long long> a;
vector<long long> temp;

long long mergeSort(int left, int right) {
    if (left >= right) {
        return 0;
    }
    int middle = (left + right) / 2;
    long long count = mergeSort(left, middle) + mergeSort(middle + 1, right);

    int i = left;
    int j = middle + 1;
    int k = left;
    while (i <= middle && j <= right) {
        if (a[i] <= a[j]) {
            temp[k++] = a[i++];
        } else {
            count += middle - i + 1;
            temp[k++] = a[j++];
        }
    }
    while (i <= middle) {
        temp[k++] = a[i++];
    }
    while (j <= right) {
        temp[k++] = a[j++];
    }
    for (int p = left; p <= right; ++p) {
        a[p] = temp[p];
    }
    return count;
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    cin >> n;
    a.resize(n);
    temp.resize(n);
    for (int i = 0; i < n; ++i) {
        cin >> a[i];
    }
    cout << mergeSort(0, n - 1) << endl;
    return 0;
}', '归并排序合并两个有序段时，若右边元素更小，则左边剩余元素都与它构成逆序对。时间复杂度 O(n log n)。'),
(15, 'JAVA', '// 四条用例推演（输入 -> 输出）：
// 用例1: 5 / 2 3 1 5 4 -> 3
// 用例2: 4 / 4 3 2 1 -> 6
// 用例3: 1 / 7 -> 0
// 用例4: 6 / 5 5 5 5 5 5 -> 0
import java.util.*;

public class Main {
    static long[] a;
    static long[] temp;

    static long mergeSort(int left, int right) {
        if (left >= right) {
            return 0;
        }
        int middle = (left + right) / 2;
        long count = mergeSort(left, middle) + mergeSort(middle + 1, right);

        int i = left;
        int j = middle + 1;
        int k = left;
        while (i <= middle && j <= right) {
            if (a[i] <= a[j]) {
                temp[k++] = a[i++];
            } else {
                count += middle - i + 1;
                temp[k++] = a[j++];
            }
        }
        while (i <= middle) {
            temp[k++] = a[i++];
        }
        while (j <= right) {
            temp[k++] = a[j++];
        }
        for (int p = left; p <= right; p++) {
            a[p] = temp[p];
        }
        return count;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        a = new long[n];
        temp = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }
        System.out.println(mergeSort(0, n - 1));
    }
}', '归并排序合并两个有序段时，若右边元素更小，则左边剩余元素都与它构成逆序对。时间复杂度 O(n log n)。')
ON DUPLICATE KEY UPDATE content=VALUES(content), explanation=VALUES(explanation);

COMMIT;

-- 验收查询：
-- SELECT knowledge_point_id, COUNT(*) FROM question GROUP BY knowledge_point_id;
-- SELECT question_id, COUNT(*) FROM question_test_case GROUP BY question_id;
-- SELECT question_id, COUNT(*) FROM question_solution GROUP BY question_id;
