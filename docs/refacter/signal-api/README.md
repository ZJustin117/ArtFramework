# Signal API refacter

## Goal

收敛 ART 现有信号 API，使节点/实体、Backend、Host 和消费者使用同一套信号语义。
此前的 C1/C2 track 已移除，不再作为本项目的独立架构边界：

- `SignalBus` / `SignalGroup` 是唯一的分发与拦截 authority；
- `SignalListener` / `UiSignal` / `SignalDecision` 是唯一的核心回调模型；
- 节点局部信号、组件信号和完整 Bus 路径有明确且最小的边界；
- 兼容 API 只作为薄适配层，不保留第二套可变信号状态或第二套分发语义；公开历史 `emit` 返回签名保持兼容，结果型新代码使用 `dispatch`；
- `emit`、订阅、声明校验、生命周期清理在节点/实体和 Host 适配路径上保持一致。

## Non-goals

- 不改变信号业务含义、Backend authority 或 intent 执行规则；
- 不新增节点/surface、下游协议类型或 STS 游戏规则；
- 不在本项目中移除有明确外部消费者依赖的兼容 API，除非先完成迁移证据和兼容决策；
- 不把 `SignalNames` 的整理扩大为无关的全仓命名清理；
- 不改变已定义的精确匹配、正则匹配、注册顺序、替换和 stop 语义。

## Architecture Authority

设计约束以以下文档为准：

- [`docs/design/backend-context.md`](../../design/backend-context.md)：统一 SignalBus、广播和 authority 边界；
- [`docs/design/art-framework.md`](../../design/art-framework.md)：节点信号声明与 `connect` / `emit` API；
- [`docs/design/presentation-entity-runtime.md`](../../design/presentation-entity-runtime.md)：声明端口、运行时 dispatch 和生命周期清理；
- [`docs/design/node-signal-runtime.md`](../../design/node-signal-runtime.md)：SignalGroup、声明式连接和 action 边界；
- [`docs/development/consumer.md`](../../development/consumer.md)：公开消费者 API 和兼容约束。

## Baseline Findings

当前底层分发已统一到 `SignalBus`，但公共表面仍有以下重复：

- `SignalListener.onSignal(UiSignal)` 与 `SignalHandler.handle(Object...)` 两套回调接口；
- `emit()` 丢弃结果与 `dispatch()` 返回 `SignalDispatchResult` 两套发送入口；
- `SignalHub` 同时承担节点 scoped API 和原始完整 Bus API；
- 局部 signal 名称、节点/组件路径和正则完整路径由字符串隐式区分；
- `SignalNames` 将控件、surface、动画、FX、拖拽和生命周期事件放在一个扁平总表中。
- 当前 wire 名称保持兼容；skeleton 完成事件的冻结名称是 `finished`，不是旧文档中的
  `animation_finished`。`card_pressed` 仍是声明能力，是否由具体 Host 产生由该 Host 合约决定。

## Checkpoint

- Project: `signal-api`
- Started: 2026-08-18
- State: complete through `SA-13`
- Active slice: none
- Last verification: full `./scripts/with-art-env.sh test` passed 720/720; `SA-13` final review
  PASS; `git diff --check` pending final current-worktree check
- Next action: no signal-api refacter work remains. Start a new bounded project for any newly
  discovered independent signal ownership or compatibility concern.

## Review Tree

按深度优先、一次一个 ledger row 审查：

1. `SignalListener` / `SignalHandler` 回调 authority 和迁移边界
2. `emit` / `dispatch` 返回值、替换和 stop 结果契约
3. `SignalHub` scoped 节点 API 与 raw Bus API 的职责边界
4. signal declaration、`SignalPaths` 和节点/实体生命周期校验
5. `SignalNames` 语义分组和命名兼容策略
6. Backend、Host、UiOps、inspect 和实体适配器的调用方迁移
7. reset、close、unmount、重建和订阅清理
8. 全仓负面清单、消费者文档和最终验证
9. ECS entity/context 销毁、重建与 signal subscription 生命周期（`SA-09`）
10. UiOps C1 sugar handler registration lifetime and window retirement（`SA-10`）
11. UiLabListeners target retirement and recreation（`SA-11`）
12. ECS signal context/entity identity ownership（`SA-12`）
13. Scoped signal wire-path legal-character grammar and canonicalization（`SA-13`）

## SA-13 Wire Grammar

`SignalPaths` is the authority for scoped signal routing names. Its canonical wire grammar is:

- component, window, and local signal names are one identifier segment matching
  `[A-Za-z0-9._-]+`;
- C1 node paths are one or more identifier segments separated by `/`;
- public scoped inputs are trimmed before validation and route using that trimmed value;
- embedded whitespace, empty segments, leading/trailing `/`, and other path metacharacters are
  rejected rather than producing ambiguous routes.

This applies only to routes constructed by `SignalPaths`, including scoped node/component signal
declarations. Raw `UiSignal` names and raw/regex `SignalBus` subscriptions remain intentionally
unrestricted compatibility APIs.

新发现的独立 ownership 问题必须新增 `SA-*` ledger 行，不得静默并入当前切片。

## Verification Policy

- 任何行为变化先添加或细化 focused JUnit 4；
- 每个纯 API/runtime 切片完成后运行 `@junit-test`，记录具体测试类和结果；
- 若修改 `tools/art-verify` 或 fixture YAML，再运行离线 `@art-verify`；
- 若触及 STS hook、native host 或绘制生命周期，先通过 JUnit，再执行适用的部署和 D1 验证；
- review 不替代测试，测试通过也不替代 review；
- 完成前做旧接口、旧 writer、重复注册表、绕过 API 和 stale 文档的负面搜索。

## Completion Definition

每个 ledger row 只有在以下条件全部满足后才能标记 `complete`：

- authority 和 ownership 边界明确；
- 旧实现已删除，或有证据证明它只是非权威兼容适配；
- focused tests 覆盖正常发送、拒绝/替换、订阅清理和适用生命周期；
- 适用的完整 semantic/tooling/device gates 通过；
- 所有接受的 review findings 已修复并复审；
- 最终 diff 未超出冻结 review scope；
- 本文件记录下一行，或明确声明项目完成。
