#!/usr/bin/env node
/**
 * 开发期自测脚本（不属于运行交付物：页面不引用它，删除整个 tools/ 目录不影响游戏）。
 * 用法：node tools/selftest.mjs
 * 原理：从 index.html 抽取 <script id="tetris-core"> 区块，在 node:vm 沙箱中执行，
 *       对内核纯逻辑（消行/碰撞/SRS 踢墙/计分/7-bag/模式判定/状态机）跑断言。
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import vm from 'node:vm';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const html = readFileSync(path.join(root, 'index.html'), 'utf8');

function extractScript(id) {
  const re = new RegExp('<script id="' + id + '">([\\s\\S]*?)<\\/script>');
  const m = html.match(re);
  if (!m) throw new Error('未找到 <script id="' + id + '"> 区块');
  return m[1];
}

const readme = readFileSync(path.join(root, 'README.md'), 'utf8');

// ---- 1) 在沙箱中执行内核区块 ----
const sandbox = { window: {}, console };
sandbox.window.window = sandbox.window;
const ctx = vm.createContext(sandbox);

let coreError = null;
try {
  vm.runInContext(extractScript('tetris-core'), ctx, { filename: 'tetris-core.js' });
} catch (e) {
  coreError = e;
}

// 内核顶层声明位于该 realm 的全局词法作用域：后续脚本可直接读取，用于自测取用内部对象。
// 用 typeof 保护，未实现到该任务的模块取到 undefined，不影响已有断言。
function expose(names) {
  const body = names
    .map(function (n) { return n + ': typeof ' + n + " !== 'undefined' ? " + n + ' : undefined'; })
    .join(', ');
  return vm.runInContext('({' + body + '})', ctx, { filename: 'expose-core.js' });
}

const core = expose(['CONST', 'Tetromino', 'Board', 'Randomizer', 'Scoring', 'Modes', 'GameState']);
const { Board, Tetromino, Randomizer, Scoring, Modes, GameState, CONST } = core;

let passed = 0;
const failures = [];

function ok(name, cond, extra) {
  if (cond) { passed++; return; }
  failures.push(name + (extra ? ' :: ' + extra : ''));
}
function eq(name, actual, expected) {
  ok(name, JSON.stringify(actual) === JSON.stringify(expected),
    'actual=' + JSON.stringify(actual) + ' expected=' + JSON.stringify(expected));
}
function sortedCells(cells) {
  return cells.map(function (c) { return c.x + ',' + c.y; }).sort();
}

ok('内核脚本区块可执行且无错误', coreError === null, coreError && coreError.message);
ok('index.html 含外壳脚本区块', /<script id="tetris-shell">[\s\S]*<\/script>/.test(html));
['board', 'next', 'hold', 'overlay', 'overlay-title', 'overlay-sub', 'mode-list', 'level-list', 'score', 'level', 'lines', 'progress-label', 'progress']
  .forEach(function (id) {
    ok('存在元素 #' + id, new RegExp('id="' + id + '"').test(html));
  });

// ===== Task 2: CONST / Tetromino =====
eq('7 种方块', Tetromino.TYPES.length, 7);
Tetromino.TYPES.forEach(function (t) {
  const shape = Tetromino.SHAPES[t];
  eq(t + ' 有 4 个旋转态', shape.rotations.length, 4);
  for (let r = 0; r < 4; r++) {
    const list = Tetromino.cells(t, r);
    eq(t + ' 旋转态 ' + r + ' 为 4 格', list.length, 4);
    ok(t + ' 旋转态 ' + r + ' 无重复格', sortedCells(list).length === new Set(sortedCells(list)).size);
  }
  ok(t + ' 颜色已定义', typeof CONST.COLORS[t] === 'string');
});
eq('O 四个旋转态相同', sortedCells(Tetromino.cells('O', 0)), sortedCells(Tetromino.cells('O', 3)));
eq('I 旋转态 1 为竖条', sortedCells(Tetromino.cells('I', 1)), ['2,0', '2,1', '2,2', '2,3']);
eq('T 旋转态 1 正确', sortedCells(Tetromino.cells('T', 1)), ['1,0', '1,1', '1,2', '2,1']);
eq('旋转态 5 回绕到 1', sortedCells(Tetromino.cells('T', 5)), sortedCells(Tetromino.cells('T', 1)));
const TRANSITIONS = [[0, 1], [1, 0], [1, 2], [2, 1], [2, 3], [3, 2], [3, 0], [0, 3]];
['I', 'T', 'S', 'Z', 'J', 'L'].forEach(function (t) {
  TRANSITIONS.forEach(function (tr) {
    const kicks = Tetromino.kicksFor(t, tr[0], tr[1]);
    eq(t + ' ' + tr[0] + '>' + tr[1] + ' 踢墙候选 5 个', kicks.length, 5);
    eq(t + ' ' + tr[0] + '>' + tr[1] + ' 首个候选为 (0,0)', kicks[0], [0, 0]);
    ok(t + ' ' + tr[0] + '>' + tr[1] + ' 候选位移在 SRS 量级内', kicks.every(function (k) {
      return Math.abs(k[0]) <= 2 && Math.abs(k[1]) <= 2;
    }));
  });
});
['I', 'O', 'T', 'S', 'Z', 'J', 'L'].forEach(function (t) {
  ok(t + ' 出生列在界内', Tetromino.spawnX(t) >= 0 && Tetromino.spawnX(t) < CONST.COLS);
});
eq('I 的 0>1 第 2 候选为 (-2,0)', Tetromino.kicksFor('I', 0, 1)[1], [-2, 0]);
eq('O 不旋转（唯一候选）', Tetromino.kicksFor('O', 0, 1), [[0, 0]]);

// ===== Task 3: Board =====
eq('棋盘高度 = ROWS + HIDDEN_ROWS', Board.TOTAL_ROWS, 22);
eq('新棋盘 22 行', Board.create().length, 22);
ok('新棋盘全空', Board.create().every(function (row) { return row.every(function (v) { return v === null; }); }));
ok('越左越界碰撞', Board.collides(Board.create(), 'O', -1, 0, 0) === true);
ok('越右越界碰撞', Board.collides(Board.create(), 'O', 9, 0, 0) === true);
ok('越底越界碰撞', Board.collides(Board.create(), 'O', 0, 21, 0) === true);
ok('隐藏缓冲区上方（y<0）不算越界', Board.collides(Board.create(), 'O', 0, -1, 0) === false);
ok('空棋盘出生点不碰撞', Board.collides(Board.create(), 'T', Tetromino.spawnX('T'), Tetromino.spawnY(), 0) === false);

let occupied = Board.create();
occupied[21][0] = 'I';
ok('与已固定格重叠碰撞', Board.collides(occupied, 'O', 0, 20, 0) === true);
ok('与已固定格相邻不碰撞', Board.collides(occupied, 'O', 1, 20, 0) === false);

const mergeBase = Board.create();
const merged = Board.merge(mergeBase, 'O', 4, 20, 0);
eq('合入后 4 格', merged[20].filter(function (v) { return v !== null; }).length + merged[21].filter(function (v) { return v !== null; }).length, 4);
eq('合入类型正确', merged[20][4], 'O');
ok('merge 不修改入参棋盘', mergeBase.every(function (row) { return row.every(function (v) { return v === null; }); }) && merged !== mergeBase);

// 单行消除
const oneRow = Board.create();
for (let c = 0; c < 10; c++) oneRow[21][c] = 'J';
oneRow[20][3] = 'S';
const clearedOne = Board.clearLines(oneRow);
eq('单行消除 cleared=1', clearedOne.cleared, 1);
eq('消除行索引', clearedOne.rows, [21]);
ok('上方方块整体下移', clearedOne.board[21][3] === 'S');
ok('消除后顶行为空', Board.isEmptyRow(clearedOne.board, 0));

// 四行同时消除
const fourRows = Board.create();
for (let r = 18; r < 22; r++) for (let c = 0; c < 10; c++) fourRows[r][c] = 'Z';
fourRows[17][0] = 'T';
const clearedFour = Board.clearLines(fourRows);
eq('四行消除 cleared=4', clearedFour.cleared, 4);
eq('四行消除索引', clearedFour.rows, [18, 19, 20, 21]);
ok('四行消除后 4 行下移', clearedFour.board[21][0] === 'T' && clearedFour.board[20][0] === null);
ok('四行消除后棋盘尺寸不变', clearedFour.board.length === 22 && clearedFour.board[0].length === 10);

// 不满行不清除
const notFull = Board.create();
notFull[21][9] = null;
for (let c = 0; c < 9; c++) notFull[21][c] = 'L';
eq('未满行 cleared=0', Board.clearLines(notFull).cleared, 0);
eq('未满行无索引', Board.clearLines(notFull).rows, []);


// ===== Task 4: Randomizer =====
const rngA = Randomizer.create(20261016);
const draws = [];
for (let i = 0; i < 70; i++) draws.push(rngA.next());
eq('70 次抽取共 70 个', draws.length, 70);
ok('每个 7 连段恰为 7 种各一次',
  [0, 1, 2, 3, 4, 5, 6, 7, 8, 9].every(function (b) {
    const seg = draws.slice(b * 7, b * 7 + 7);
    return new Set(seg).size === 7 && seg.every(function (t) { return Tetromino.TYPES.indexOf(t) >= 0; });
  }));
Tetromino.TYPES.forEach(function (t) {
  eq('70 次抽取中 ' + t + ' 出现 10 次', draws.filter(function (d) { return d === t; }).length, 10);
});

const rngB = Randomizer.create(20261016);
const drawsB = [];
for (let i = 0; i < 14; i++) drawsB.push(rngB.next());
eq('同种子序列一致', drawsB, draws.slice(0, 14));

const rngC = Randomizer.create(999);
const drawsC = [];
for (let i = 0; i < 14; i++) drawsC.push(rngC.next());
ok('不同种子序列不同', JSON.stringify(drawsC) !== JSON.stringify(drawsB));

const rngD = Randomizer.create(7);
const preview = rngD.peek(3);
eq('peek 返回 3 个', preview.length, 3);
eq('peek 不消费队列', rngD.peek(3), preview);
eq('peek 与后续 next 顺序一致', [rngD.next(), rngD.next(), rngD.next()], preview);

// ===== Task 5: Scoring =====
eq('单行 100 x 等级', Scoring.linePoints(1, 1), 100);
eq('四行 800 x 等级', Scoring.linePoints(4, 3), 2400);
eq('零行不加分', Scoring.linePoints(0, 5), 0);
eq('软降 1 分/格', Scoring.softDropPoints(7), 7);
eq('硬降 2 分/格', Scoring.hardDropPoints(7), 14);
eq('每 10 行升 1 级', Scoring.levelFor(10, 1), 2);
eq('起始等级作为基准', Scoring.levelFor(0, 15), 15);
eq('起始等级 5 消 25 行', Scoring.levelFor(25, 5), 7);
eq('等级 1 重力 1000ms', Scoring.gravityMs(1), 1000);
ok('等级越高越快', Scoring.gravityMs(1) > Scoring.gravityMs(2) && Scoring.gravityMs(2) > Scoring.gravityMs(5) && Scoring.gravityMs(5) > Scoring.gravityMs(10));
ok('高速档不低于下限', Scoring.gravityMs(10) >= 16 && Scoring.gravityMs(15) === 16 && Scoring.gravityMs(20) === 16);
eq('消行累计分数与等级', Scoring.applyClear({ score: 0, lines: 0, level: 1, startLevel: 1 }, 4), { score: 800, lines: 4, level: 1 });
eq('达 10 行升级', Scoring.applyClear({ score: 800, lines: 6, level: 1, startLevel: 1 }, 4), { score: 1600, lines: 10, level: 2 });
eq('升级后按新等级计分', Scoring.applyClear({ score: 1600, lines: 10, level: 2, startLevel: 1 }, 1), { score: 1800, lines: 11, level: 2 });

// ===== Task 6: Modes =====
eq('三种模式且顺序固定', Modes.list.map(function (m) { return m.id; }), ['marathon', 'sprint', 'ultra']);
eq('byId 命中', Modes.byId('sprint').id, 'sprint');
eq('byId 未命中回退首个', Modes.byId('nope').id, 'marathon');
eq('mm:ss.cc 格式 0', Modes.formatMs(0), '00:00.00');
eq('mm:ss.cc 格式 61234', Modes.formatMs(61234), '01:01.23');
eq('mm:ss.cc 负数归零', Modes.formatMs(-5), '00:00.00');

const marathon = Modes.byId('marathon');
const sprint = Modes.byId('sprint');
const ultra = Modes.byId('ultra');
ok('马拉松永不达成目标', marathon.isFinished({ score: 999999, lines: 999, elapsedMs: 1e9, level: 20, topOut: false }) === false);
ok('40 行未满未结束', sprint.isFinished({ lines: 39, elapsedMs: 1000, score: 0, level: 1, topOut: false }) === false);
ok('40 行满即结束', sprint.isFinished({ lines: 40, elapsedMs: 1000, score: 0, level: 1, topOut: false }) === true);
ok('Ultra 未到点未结束', ultra.isFinished({ elapsedMs: 119999, lines: 0, score: 0, level: 1, topOut: false }) === false);
ok('Ultra 到点即结束', ultra.isFinished({ elapsedMs: 120000, lines: 0, score: 0, level: 1, topOut: false }) === true);
eq('Sprint 结算含用时', sprint.resultText({ lines: 40, elapsedMs: 91234, score: 1200, level: 3, topOut: false }), '用时 01:31.23｜消行 40');
eq('Ultra 结算含总分', ultra.resultText({ lines: 12, elapsedMs: 120000, score: 3400, level: 2, topOut: false }), '总分 3400｜消行 12');
eq('Ultra 进度为倒计时', ultra.progressText({ elapsedMs: 30000, lines: 0, score: 0, level: 1, topOut: false }), '01:30.00');
eq('Ultra 进度不为负', ultra.progressText({ elapsedMs: 130000, lines: 0, score: 0, level: 1, topOut: false }), '00:00.00');
ok('堆顶即结束（马拉松也是）', Modes.isOver(marathon, { topOut: true, lines: 0, elapsedMs: 0, score: 0, level: 1 }) === true);
ok('未堆顶且未达标则不结束', Modes.isOver(sprint, { topOut: false, lines: 3, elapsedMs: 5000, score: 0, level: 1 }) === false);
// ===== Task 7: GameState =====
eq('公开导出恰好 5 个模块', Object.keys(sandbox.window.Tetris).sort(), ['Board', 'Modes', 'Randomizer', 'Scoring', 'Tetromino']);
ok('导出对象可调用', sandbox.window.Tetris.Board === Board && sandbox.window.Tetris.Modes === Modes);

const gs = GameState.start(GameState.create(), 'marathon', 5, 42);
eq('开局状态为 PLAYING', gs.status, 'PLAYING');
eq('起始等级生效', gs.level, 5);
eq('重力按起始等级', gs.dropIntervalMs, Scoring.gravityMs(5));
eq('Next 队列 3 个', gs.next.length, 3);
ok('当前方块已生成', gs.piece !== null && Tetromino.TYPES.indexOf(gs.piece.type) >= 0);
eq('开局分数为 0', gs.score, 0);

const gsSeedA = GameState.start(GameState.create(), 'marathon', 1, 1234);
const gsSeedB = GameState.start(GameState.create(), 'marathon', 1, 1234);
eq('同种子首块一致', gsSeedA.piece.type, gsSeedB.piece.type);
eq('同种子队列一致', gsSeedA.next, gsSeedB.next);

// 左右移动与边界
const gsMove = GameState.start(GameState.create(), 'marathon', 1, 7);
const x0 = gsMove.piece.x;
GameState.applyIntent(gsMove, { type: 'move', dx: -1 });
eq('左移一格', gsMove.piece.x, x0 - 1);
for (let i = 0; i < 20; i++) GameState.applyIntent(gsMove, { type: 'move', dx: -1 });
ok('不越左墙', gsMove.piece.x >= 0);
for (let i = 0; i < 20; i++) GameState.applyIntent(gsMove, { type: 'move', dx: 1 });
ok('不越右墙', gsMove.piece.x + 3 <= 10);

// 旋转（含回绕）
const gsRot = GameState.start(GameState.create(), 'marathon', 1, 8);
GameState.spawn(gsRot, 'T');
GameState.applyIntent(gsRot, { type: 'rotate', dir: 1 });
eq('顺时针旋转到 1', gsRot.piece.rot, 1);
GameState.applyIntent(gsRot, { type: 'rotate', dir: -1 });
eq('逆时针回到 0', gsRot.piece.rot, 0);

// SRS 踢墙：I 块立起贴左墙后可借 (2,0) 踢墙转平（I 竖条占包围盒第 2 列 -> x=-2 即贴左墙）
const gsKick = GameState.start(GameState.create(), 'marathon', 1, 9);
GameState.spawn(gsKick, 'I');
gsKick.piece.rot = 1;
gsKick.piece.x = -2;
ok('竖 I 贴左墙不碰撞', Board.collides(gsKick.board, 'I', -2, gsKick.piece.y, 1) === false);
ok('直接转平会碰撞', Board.collides(gsKick.board, 'I', -2, gsKick.piece.y, 0) === true);
ok('踢墙转平成功', GameState.tryRotate(gsKick, -1) === true);
eq('踢墙后旋转态为 0', gsKick.piece.rot, 0);
eq('踢墙后借用 +2 位移', gsKick.piece.x, 0);

// 踢墙全失败时保持原姿态：只留 1 格宽竖井，任何旋转都不可能成功
const gsStuck = GameState.start(GameState.create(), 'marathon', 1, 10);
GameState.spawn(gsStuck, 'I');
for (let r = 0; r < Board.TOTAL_ROWS; r++) {
  for (let c = 0; c < CONST.COLS; c++) gsStuck.board[r][c] = (c === 4) ? null : 'Z';
}
gsStuck.piece.rot = 1;
gsStuck.piece.x = 2; // 竖 I（包围盒第 2 列）正好落在第 4 列
gsStuck.piece.y = 10;
ok('竖井中当前姿态不碰撞', Board.collides(gsStuck.board, 'I', 2, 10, 1) === false);
eq('竖井中旋转失败（踢墙候选全部碰撞）', GameState.tryRotate(gsStuck, 1), false);
eq('失败后旋转态不变', gsStuck.piece.rot, 1);
eq('失败后位置不变', [gsStuck.piece.x, gsStuck.piece.y], [2, 10]);

// 软降：加速下落 + 加分
const gsSoft = GameState.start(GameState.create(), 'marathon', 1, 11);
GameState.applyIntent(gsSoft, { type: 'softDrop', on: true });
GameState.update(gsSoft, 250);
ok('开启软降后 250ms 内即下落', gsSoft.piece.y > 0);
ok('软降加分（1 分/格）', gsSoft.score > 0);
GameState.applyIntent(gsSoft, { type: 'softDrop', on: false });
ok('关闭软降后状态还原', gsSoft.softDropping === false);

// 硬降：落到幽灵位置、加分、立即锁定并生成新块
const gsHard = GameState.start(GameState.create(), 'marathon', 1, 12);
const ghost = GameState.ghostY(gsHard);
const hardCells = GameState.hardDrop(gsHard);
eq('硬降格数 = 幽灵位移', hardCells, ghost - Tetromino.spawnY());
eq('硬降加分 2/格', gsHard.score, hardCells * 2);
ok('硬降后已合入棋盘', gsHard.board[21].some(function (v) { return v !== null; }) || gsHard.board[20].some(function (v) { return v !== null; }));
ok('硬降后立即生成新块', gsHard.piece !== null);

// 锁定延迟：贴地后 300ms 未锁定、再 300ms 锁定
const gsLock = GameState.start(GameState.create(), 'marathon', 1, 13);
GameState.spawn(gsLock, 'O');
gsLock.piece.y = 20; // 直接贴底
GameState.update(gsLock, 250);
ok('锁定延迟内仍可操作', gsLock.piece !== null && gsLock.status === 'PLAYING');
GameState.update(gsLock, 250);
GameState.update(gsLock, 250);
ok('超过锁定延迟后棋盘出现固定方块', gsLock.board[21].filter(function (v) { return v !== null; }).length >= 2);

// 消行动画与计分：底行只差 3..6 列，横躺的 I 硬降后正好补满
const gsClear = GameState.start(GameState.create(), 'marathon', 1, 14);
[0, 1, 2, 7, 8, 9].forEach(function (c) { gsClear.board[21][c] = 'J'; });
GameState.spawn(gsClear, 'I'); // 出生姿态横躺：包围盒第 1 行、第 0..3 列 -> 棋盘第 3..6 列
GameState.hardDrop(gsClear);
ok('触发消行动画', gsClear.clearing !== null && gsClear.clearing.rows.indexOf(21) >= 0);
GameState.update(gsClear, 250);
ok('动画结束后清行', gsClear.clearing === null && gsClear.lines === 1);
ok('单行消除加分 100 x 等级', gsClear.score >= 100);

// Hold：首次存入、二次取回、同一方块落地前不可重复
const gsHold = GameState.start(GameState.create(), 'marathon', 1, 15);
const firstType = gsHold.piece.type;
ok('首次 Hold 成功', GameState.hold(gsHold) === true);
eq('Hold 槽记录首块', gsHold.hold, firstType);
ok('同一方块落地前不可重复 Hold', GameState.hold(gsHold) === false);
GameState.spawn(gsHold); // 模拟落地后新块
const typeSecond = gsHold.piece.type;
ok('落地后可再次 Hold', GameState.hold(gsHold) === true);
eq('取回 Hold 中的方块', gsHold.piece.type, firstType);
eq('Hold 槽换为原方块', gsHold.hold, typeSecond);

// 暂停：冻结推进与计时
const gsPause = GameState.start(GameState.create(), 'marathon', 1, 16);
GameState.applyIntent(gsPause, { type: 'pause' });
eq('暂停状态', gsPause.status, 'PAUSED');
const yBefore = gsPause.piece.y;
const elapsedBefore = gsPause.elapsedMs;
GameState.update(gsPause, 250);
eq('暂停期间方块冻结', gsPause.piece.y, yBefore);
eq('暂停期间计时不累加', gsPause.elapsedMs, elapsedBefore);
GameState.applyIntent(gsPause, { type: 'pause' });
eq('恢复为 PLAYING', gsPause.status, 'PLAYING');

// 堆顶结束
const gsTop = GameState.start(GameState.create(), 'marathon', 1, 17);
for (let r = 0; r < 4; r++) for (let c = 0; c < 10; c++) gsTop.board[r][c] = 'S';
GameState.spawn(gsTop, 'O');
eq('出生点碰撞即结束', gsTop.status, 'GAME_OVER');
eq('标记堆顶', gsTop.topOut, true);
ok('结束信息非空', gsTop.message.length > 0);
ok('结束态可回到 READY', GameState.toReady(gsTop).status === 'READY');

// 40 行竞速：消满 40 行即结束
const gsSprint = GameState.start(GameState.create(), 'sprint', 1, 18);
gsSprint.lines = 40;
GameState.update(gsSprint, 16);
eq('Sprint 达标即结算', gsSprint.status, 'GAME_OVER');

// 限时冲刺：到点即结束
const gsUltra = GameState.start(GameState.create(), 'ultra', 1, 19);
gsUltra.elapsedMs = CONST.ULTRA_MS - 100;
GameState.update(gsUltra, 250);
eq('Ultra 到点即结算', gsUltra.status, 'GAME_OVER');
ok('Ultra 结算含总分', Modes.byId('ultra').resultText(gsUltra).indexOf('总分') >= 0);

eq('目标达成不标记堆顶（Sprint）', gsSprint.topOut, false);
eq('Sprint 达成提示为达成语', gsSprint.message, '40 行达成');
eq('目标达成不标记堆顶（Ultra）', gsUltra.topOut, false);
eq('Ultra 到点提示为时间到', gsUltra.message, '时间到');

// dt 钳制：一次超大 dt 只按上限 250ms 推进
const gsDt = GameState.start(GameState.create(), 'marathon', 1, 20);
const gsDtBefore = gsDt.elapsedMs;
GameState.update(gsDt, 100000);
eq('超大 dt 被钳制到 MAX_DT_MS', gsDt.elapsedMs - gsDtBefore, CONST.MAX_DT_MS);
eq('超大 dt 未触发锁定（累加器保留）', gsDt.dropAccMs, CONST.MAX_DT_MS);
// ===== Task 8: 外壳结构（静态断言；外壳依赖 DOM，无法在 Node 中执行）=====
ok('已定义渲染入口 drawBoard', /function drawBoard\(state\)/.test(html));
ok('已定义幽灵块绘制', /drawGhostCell/.test(html));
ok('已定义 Next/Hold 绘制', /function drawNext\(state\)/.test(html) && /function drawHold\(state\)/.test(html));
ok('侧栏数字与模式进度已接线', /progressLabel\.textContent/.test(html) && /progressText\(state\)/.test(html));
ok('输入归一化为 intent', /function intentFor\(action\)/.test(html) && /type: 'hardDrop'/.test(html));
ok('DAS/ARR 由 poll 驱动', /function poll\(dtMs\)/.test(html) && /CONST\.DAS_MS/.test(html) && /CONST\.ARR_MS/.test(html));
ok('旋转/硬降忽略系统自动重复', /if \(isRepeat\) return null; \/\/ 旋转\/硬降\/暂存只响应按下沿/.test(html));
// ===== Task 9: 接线与验收标准 1/2/19/20 的静态合规断言 =====
ok('无 type="module"（file:// 可用）', !/type\s*=\s*"module"/.test(html));
ok('无外部脚本引用', !/<script[^>]+src=/i.test(html));
ok('无外链样式/媒体资源', !/<link[^>]+href=/i.test(html) && !/<(img|audio|video)[^>]+src=/i.test(html));
ok('无 http/https 远程地址', !/https?:\/\//i.test(html));
ok('无触控事件监听', !/touch(start|move|end|cancel)|pointerdown|pointerup|ontouch/i.test(html));
ok('无本地存储 / Cookie 依赖', !/localStorage|sessionStorage|document\.cookie/.test(html));
ok('主循环使用 requestAnimationFrame', /requestAnimationFrame\(loop\)/.test(html));
ok('游戏按键统一 preventDefault', /if \(ALL_CODES\.indexOf\(code\) >= 0\) e\.preventDefault\(\);/.test(html));
ok('失焦自动暂停已接线', /addEventListener\('blur', autoPause\)/.test(html));
ok('切后台自动暂停已接线', /addEventListener\('visibilitychange'/.test(html) && /document\.hidden/.test(html));
ok('resize 重算画布', /addEventListener\('resize'/.test(html) && /function layout\(\)/.test(html));
ok('READY 面板可键鼠双通道操作', /onReadyNavigation/.test(html) && /addEventListener\('click'/.test(html));
ok('最高分只在内存中（无持久化写入）', /ui\.bestScore/.test(html) && !/bestScore.*localStorage/.test(html));
ok('DPR 适配', /devicePixelRatio/.test(html));
// ===== Task 10: 交付物完整性与 README 覆盖度 =====
ok('README.md 存在且非空', readme.length > 200);
ok('README 覆盖打开方式', /如何打开/.test(readme) && /index\.html/.test(readme));
ok('README 覆盖完整键位表', ['左移', '硬降', '暂存', '暂停', '顺时针', '逆时针'].every(function (k) { return readme.indexOf(k) >= 0; }));
ok('README 覆盖三种模式', /马拉松/.test(readme) && /40 行竞速/.test(readme) && /限时冲刺/.test(readme));
ok('README 覆盖可调参数（CONST）', /CONST/.test(readme) && /LOCK_DELAY_MS/.test(readme) && /SPRINT_LINES/.test(readme));
ok('README 说明不做持久化', /持久化/.test(readme));
ok('页面不引用 README', !/README\.md/.test(html));
ok('页面不引用 tools/', !/tools\//.test(html));
// @@ASSERTIONS@@
// ===== 外壳行为冒烟：零依赖 DOM 桩中真跑「主循环 / 键盘输入 / Renderer→DOM」管线 =====
function createDomStub() {
  const ctxStub = new Proxy({}, {
    get(target, key) {
      if (key in target) return target[key];
      if (typeof key === 'symbol') return undefined;
      return function () { return { addColorStop() {} }; };
    },
    set(target, key, value) { target[key] = value; return true; }
  });

  function element(id) {
    return {
      id: id,
      textContent: '',
      className: '',
      dataset: {},
      children: [],
      style: {},
      width: 0,
      height: 0,
      _handlers: {},
      classList: {
        add(c) { this._set.add(c); },
        remove(c) { this._set.delete(c); },
        contains(c) { return this._set.has(c); },
        toggle(c, on) {
          if (on === undefined) on = !this._set.has(c);
          if (on) this._set.add(c); else this._set.delete(c);
          return on;
        },
        _set: new Set()
      },
      appendChild(node) { this.children.push(node); return node; },
      addEventListener(type, fn) { this._handlers[type] = (this._handlers[type] || []).concat(fn); },
      blur() {},
      getContext() { return ctxStub; },
      dispatch(type, ev) { (this._handlers[type] || []).forEach(function (fn) { fn(ev); }); }
    };
  }

  const doc = {
    hidden: false,
    _els: {},
    _handlers: {},
    getElementById(id) { return this._els[id] || (this._els[id] = element(id)); },
    createElement() { return element(''); },
    addEventListener(type, fn) { this._handlers[type] = (this._handlers[type] || []).concat(fn); }
  };
  // 预注册 bootstrap 会取用的元素，便于断言直接触达
  ['board', 'next', 'hold', 'overlay', 'overlay-title', 'overlay-sub', 'mode-list', 'level-list',
    'score', 'level', 'lines', 'progress-label', 'progress'].forEach(function (id) {
    doc.getElementById(id);
  });

  const win = {
    _handlers: {},
    devicePixelRatio: 2,
    document: doc,
    addEventListener(type, fn) { this._handlers[type] = (this._handlers[type] || []).concat(fn); }
  };
  win.window = win;

  const frames = { queue: [], errors: [], count: 0, nextId: 0 };

  function requestAnimationFrame(cb) {
    frames.queue.push(cb);
    frames.count++;
    return ++frames.nextId;
  }

  function pump(ts) {
    if (frames.queue.length === 0) return false;
    const cb = frames.queue.shift();
    try { cb(ts); } catch (e) { frames.errors.push(e); }
    return true;
  }

  function pressKey(code, opts) {
    const options = opts || {};
    const ev = { code: code, repeat: false, type: 'keydown', preventDefault() {} };
    (win._handlers.keydown || []).forEach(function (fn) { fn(ev); });
    if (options.up !== false) (win._handlers.keyup || []).forEach(function (fn) { fn(ev); });
  }

  const globals = { window: win, document: doc, requestAnimationFrame: requestAnimationFrame, console: console };
  return { globals: globals, pump: pump, frames: frames, pressKey: pressKey, doc: doc };
}

if (coreError === null) {
  const stub = createDomStub();
  const doc = stub.doc;
  const shellCtx = vm.createContext(stub.globals);

  let shellLoadError = null;
  try {
    // 外壳需要内核的 CONST/Board/Modes/GameState，故两个区块跑在同一个 context 里
    vm.runInContext(extractScript('tetris-core'), shellCtx, { filename: 'smoke-core.js' });
    vm.runInContext(extractScript('tetris-shell'), shellCtx, { filename: 'smoke-shell.js' });
  } catch (e) {
    shellLoadError = e;
  }
  ok('外壳可在 DOM 桩中加载并执行', shellLoadError === null, shellLoadError && shellLoadError.stack);

  // 首帧回归：READY（board === null）时主循环必须活下来（Fix A）
  stub.pump(0);
  stub.pump(16);
  stub.pump(32);
  // eq 只有 (name, actual, expected) 三个形参，这里用 ok 以便失败时直接带出首个异常栈
  ok('READY 态首帧不抛错', stub.frames.errors.length === 0, stub.frames.errors[0] && stub.frames.errors[0].stack);
  ok('主循环持续续排 rAF', stub.frames.count >= 4, stub.frames.count);

  eq('READY 面板模式按钮 3 个', doc.getElementById('mode-list').children.length, 3);
  eq('READY 面板等级按钮 15 个', doc.getElementById('level-list').children.length, 15);
  eq('初始遮罩可见', doc.getElementById('overlay').classList.contains('hidden'), false);

  stub.pressKey('ArrowRight'); // READY 面板：切到 sprint
  stub.pressKey('Enter'); // 开局
  stub.pump(48);
  eq('Enter 开局后遮罩隐藏', doc.getElementById('overlay').classList.contains('hidden'), true);
  eq('sprint 模式进度标签', doc.getElementById('progress-label').textContent, '用时');

  const smokeScoreBefore = Number(doc.getElementById('score').textContent);
  stub.pressKey('Space'); // 硬降
  stub.pump(64);
  ok('硬降经输入管线加分', Number(doc.getElementById('score').textContent) > smokeScoreBefore,
    smokeScoreBefore + ' -> ' + doc.getElementById('score').textContent);

  stub.pressKey('KeyP'); // 暂停
  stub.pump(80);
  eq('暂停后标题', doc.getElementById('overlay-title').textContent, '已暂停');

  eq('冒烟全程无异常', stub.frames.errors.length, 0);
}

// ---- 汇总 ----
console.log('selftest: ' + passed + ' passed, ' + failures.length + ' failed');
if (failures.length > 0) {
  failures.forEach(function (f) { console.error('  FAIL ' + f); });
  process.exit(1);
}
