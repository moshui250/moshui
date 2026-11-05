// 仅在后端题库接口尚未接入时用于展示前端骨架，页面会明确标注“演示数据”。
export const demoKnowledgePoints = [
  { id: 1, code: 'array', name: '数组', description: '下标、遍历、前缀和与双指针', questionCount: 3 },
  { id: 2, code: 'string', name: '字符串', description: '字符处理、回文与子串', questionCount: 3 },
  { id: 3, code: 'linked_list', name: '链表', description: '指针操作、反转与合并', questionCount: 3 },
  { id: 4, code: 'recursion', name: '递归', description: '递归定义、边界与记忆化', questionCount: 3 },
  { id: 5, code: 'sort', name: '排序', description: '比较、稳定性与复杂度', questionCount: 3 }
]

export const demoQuestions = [
  {
    id: 1,
    title: '两数之和',
    difficulty: 1,
    knowledgePointId: 1,
    knowledgePointName: '数组',
    type: 'PUBLIC',
    solved: true,
    description:
      '给定一个长度为 n 的整数数组和一个目标值 target，请找出数组中和为 target 的两个整数，并输出它们的下标。保证有且仅有一组答案，下标从 0 开始。',
    inputDesc: '第一行两个整数 n 和 target。\n第二行 n 个整数，表示数组元素。',
    outputDesc: '一行两个整数，依次输出较小和较大的下标。',
    samples: [
      { input: '4 9\n2 7 11 15', output: '0 1' },
      { input: '3 6\n3 2 4', output: '1 2' }
    ]
  },
  {
    id: 2,
    title: '反转链表',
    difficulty: 2,
    knowledgePointId: 3,
    knowledgePointName: '链表',
    type: 'PUBLIC',
    solved: false,
    description:
      '给定一个单链表的 n 个结点值，请将链表反转后按顺序输出所有结点的值。要求只使用常数级别的额外空间。',
    inputDesc: '第一行一个整数 n。\n第二行 n 个整数，依次表示链表结点的值。',
    outputDesc: '一行 n 个整数，表示反转后的链表。',
    samples: [
      { input: '5\n1 2 3 4 5', output: '5 4 3 2 1' },
      { input: '1\n7', output: '7' }
    ]
  },
  {
    id: 3,
    title: '最长回文子串',
    difficulty: 3,
    knowledgePointId: 2,
    knowledgePointName: '字符串',
    type: 'PUBLIC',
    solved: false,
    description:
      '给定一个只包含小写字母的字符串，请输出其中最长的回文子串。若存在多个长度相同的最长回文子串，输出最先出现的那一个。',
    inputDesc: '一行一个字符串 s。',
    outputDesc: '一行一个字符串，表示最长回文子串。',
    samples: [
      { input: 'babad', output: 'bab' },
      { input: 'cbbd', output: 'bb' }
    ]
  },
  {
    id: 4,
    title: '斐波那契数列',
    difficulty: 1,
    knowledgePointId: 4,
    knowledgePointName: '递归',
    type: 'PUBLIC',
    solved: false,
    description:
      '斐波那契数列定义为 F(0)=0，F(1)=1，F(n)=F(n-1)+F(n-2)。给定 n，请输出第 n 项的值。',
    inputDesc: '一行一个整数 n。',
    outputDesc: '一行一个整数，表示 F(n)。',
    samples: [
      { input: '6', output: '8' },
      { input: '10', output: '55' }
    ]
  },
  {
    id: 5,
    title: '数组升序排序',
    difficulty: 2,
    knowledgePointId: 5,
    knowledgePointName: '排序',
    type: 'PUBLIC',
    solved: true,
    description:
      '给定 n 个整数，请将它们按升序排列后输出。要求不使用语言内置的排序函数，练习一种 O(n log n) 的排序算法。',
    inputDesc: '第一行一个整数 n。\n第二行 n 个整数。',
    outputDesc: '一行 n 个整数，按升序排列。',
    samples: [
      { input: '5\n3 1 4 1 5', output: '1 1 3 4 5' },
      { input: '3\n9 -2 0', output: '-2 0 9' }
    ]
  },
  {
    id: 6,
    title: '合并两个有序数组',
    difficulty: 3,
    knowledgePointId: 1,
    knowledgePointName: '数组',
    type: 'PUBLIC',
    solved: false,
    description:
      '给定两个已经按升序排列的数组，请将它们合并为一个仍然有序的数组并输出。要求时间复杂度为 O(n+m)。',
    inputDesc: '第一行两个整数 n 和 m。\n第二行 n 个整数。\n第三行 m 个整数。',
    outputDesc: '一行 n+m 个整数，表示合并后的有序数组。',
    samples: [
      { input: '3 3\n1 3 5\n2 4 6', output: '1 2 3 4 5 6' },
      { input: '2 1\n1 2\n0', output: '0 1 2' }
    ]
  }
]

export const emptyProfileStat = {
  totalSubmit: 0,
  acSubmit: 0,
  accuracy: 0,
  solvedCount: 0,
  totalAc: 0,
  wrongCount: 0,
  knowledgeStats: [],
  recentSubmits: []
}
