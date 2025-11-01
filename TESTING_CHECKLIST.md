# ✅ TESTING CHECKLIST - 4 CẢI TIẾN

> Run these tests after implementing the 4 improvements

---

## 🔧 1. OPTIMIZE PostActions (remember() memoization)

**File affected**: `PostCard.kt`

### Checklist:
- [ ] **Build & Compile**: App builds successfully without errors
- [ ] **UI Render**: HomeScreen loads without crash
- [ ] **Like/Save Icons**: 
  - [ ] Like icon shows red (filled) when `isLiked = true`
  - [ ] Like icon shows outline when `isLiked = false`
  - [ ] Save icon shows blue (filled) when `isSaved = true`
  - [ ] Save icon shows outline when `isSaved = false`
- [ ] **Scroll Performance**:
  - [ ] Scroll through 20+ posts in LazyColumn
  - [ ] No visible jank or stuttering
  - [ ] Framerate stays 60fps (use Android Profiler)
- [ ] **State Updates**:
  - [ ] Click like → icon updates immediately
  - [ ] Click save → icon updates immediately
  - [ ] Colors transition smoothly

**Expected Outcome**: ⚡ Smoother scrolling, less recomposition

---

## 🙈 2. FILTER HIDDEN POSTS

**Files affected**: `HomeScreen.kt`

### Checklist:
- [ ] **Initial Load**: 
  - [ ] Load HomeScreen
  - [ ] See posts from selected category
  - [ ] Count total posts visible

- [ ] **Hide Post Action**:
  - [ ] Find a post with menu (three dots)
  - [ ] Click menu → "Ẩn bài viết"
  - [ ] [ ] Post disappears **immediately** from screen
  - [ ] Count decreases by 1

- [ ] **Filter Logic**:
  - [ ] Scroll to see remaining posts
  - [ ] Hidden post should **NOT** appear again
  - [ ] Other posts still visible

- [ ] **Persistence** (if FirebaseRepository is working):
  - [ ] Close app completely
  - [ ] Reopen app
  - [ ] Navigate back to HomeScreen
  - [ ] Hidden post should **STILL** be hidden
  - [ ] [ ] Verify hidden post didn't reappear

- [ ] **Multiple Hidden Posts**:
  - [ ] Hide 3-5 different posts
  - [ ] All should disappear from UI
  - [ ] None should reappear on scroll

**Expected Outcome**: ✅ "Ẩn bài viết" feature works correctly

---

## 🚨 3. ERROR DIALOG (Improved UX)

**Files affected**: `HomeScreen.kt`, `HomeViewModel.kt`

### Checklist - Error Display:
- [ ] **Trigger Error State** (disable internet):
  - [ ] Turn OFF wifi/mobile data
  - [ ] Kill and restart app
  - [ ] HomeScreen should show error dialog

- [ ] **Visual Elements**:
  - [ ] Error icon appears (red ErrorOutline icon)
  - [ ] Heading "Oops! Có lỗi xảy ra" is visible
  - [ ] Error message appears below heading
  - [ ] Button "Thử lại" is visible and clickable
  - [ ] All elements are centered

- [ ] **Styling**:
  - [ ] Icon is red (error color)
  - [ ] Text contrast is good (readable)
  - [ ] Button is Material Design style
  - [ ] Proper spacing between elements

### Checklist - Retry Functionality:
- [ ] **Click Retry (internet still off)**:
  - [ ] Error dialog persists
  - [ ] App doesn't crash
  - [ ] User can keep trying

- [ ] **Click Retry (internet turned ON)**:
  - [ ] Loading spinner appears
  - [ ] Posts load successfully
  - [ ] Error dialog disappears
  - [ ] LazyColumn shows posts

- [ ] **onRetry() Function**:
  - [ ] Clears error state
  - [ ] Sets isLoading = true
  - [ ] Calls loadCategoriesAndInitialPosts()

**Expected Outcome**: 🎨 Beautiful error UI with working retry

---

## ♾️ 4. INFINITE SCROLL (Load More)

**Files affected**: `HomeScreen.kt`, `HomeViewModel.kt`

### Checklist - UI Elements:
- [ ] **Initial State**:
  - [ ] HomeScreen loads normally
  - [ ] Posts display in LazyColumn
  - [ ] No "loading more" indicator at start

- [ ] **Scroll to Bottom**:
  - [ ] Scroll down through all visible posts
  - [ ] Reach the very bottom of LazyColumn
  - [ ] Loading spinner appears at bottom (CircularProgressIndicator)

- [ ] **Loading State**:
  - [ ] Spinner shows while loading
  - [ ] Size is reasonable (32.dp)
  - [ ] Padding looks good (16.dp vertical)

### Checklist - Load More Logic:
- [ ] **New Posts Load**:
  - [ ] Posts load successfully
  - [ ] Loading spinner disappears
  - [ ] New posts appear in LazyColumn
  - [ ] User can continue scrolling

- [ ] **Duplicate Prevention**:
  - [ ] After load more, check post count
  - [ ] No duplicate posts visible
  - [ ] All post IDs are unique

- [ ] **Multiple Load More Cycles**:
  - [ ] Scroll to bottom again
  - [ ] More posts load
  - [ ] Repeat 2-3 times
  - [ ] No performance degradation
  - [ ] No crashes

### Checklist - Guard Conditions:
- [ ] **No Spam Loading**:
  - [ ] Scroll to bottom
  - [ ] Don't scroll away
  - [ ] Should NOT load multiple times
  - [ ] Should check `isLoadingMore` state

- [ ] **Disable During Initial Load**:
  - [ ] If `isLoading = true`
  - [ ] Infinite scroll should NOT trigger
  - [ ] Load more should wait until initial load done

- [ ] **Empty State**:
  - [ ] If LazyColumn is empty
  - [ ] Load more should NOT trigger
  - [ ] Check `filteredPosts.isNotEmpty()`

**Expected Outcome**: 🚀 Seamless infinite scrolling experience

---

## 📊 COMBINED TEST SCENARIOS

### Scenario 1: Complete User Journey
```
1. Open app (test error dialog if network down)
2. See posts load
3. Scroll through posts (test PostActions optimization)
4. Hide 2-3 posts (test filter)
5. Scroll to bottom (test infinite scroll)
6. See more posts load
7. No hidden posts reappear
8. Verify no crashes
```

### Scenario 2: Error Recovery
```
1. Turn OFF internet
2. Open app → error dialog
3. Click "Thử lại" (internet still off)
4. Error persists (app doesn't crash)
5. Turn ON internet
6. Click "Thử lại"
7. Posts load successfully
```

### Scenario 3: Performance Test
```
1. Open app with good network
2. Scroll continuously for 30 seconds
3. Monitor for:
   - [ ] No ANR (Application Not Responding)
   - [ ] Framerate stays 60fps
   - [ ] Memory usage stable
   - [ ] No excessive recomposition (check Compose metrics)
```

---

## 🐛 POTENTIAL ISSUES TO WATCH FOR

| Issue | Symptom | Solution |
|-------|---------|----------|
| **Duplicate posts** | Same post appears twice | Check `.distinctBy()` logic |
| **Infinite loop loading** | Loading spinner never stops | Check `isLoadingMore` guard |
| **Hidden posts reappear** | Post shows again after hide | Verify Firebase persistence |
| **Error dialog not dismissing** | Can't recover from error | Check `onRetry()` implementation |
| **LazyColumn lag** | Stuttering while scrolling | Profile with Android Profiler |
| **Memory leak** | App crashes after 5+ min | Check coroutine cleanup |

---

## ✨ EXPECTED RESULTS

After all tests pass:

- ✅ **Performance**: App feels smooth, no jank
- ✅ **Features**: All 4 improvements work correctly
- ✅ **UX**: Error dialogs are clear and helpful
- ✅ **Reliability**: No crashes or ANRs
- ✅ **User Experience**: Seamless infinite scrolling

---

## 📝 TEST REPORT TEMPLATE

```markdown
### Test Results - [DATE]

#### 1. PostActions Optimization
- Build: ✅ / ❌
- UI Render: ✅ / ❌
- Icon Display: ✅ / ❌
- Performance: ✅ / ❌
- Notes: _________________

#### 2. Filter Hidden Posts
- Hide Action: ✅ / ❌
- Immediate Disappear: ✅ / ❌
- Persistence: ✅ / ❌ (if applicable)
- Notes: _________________

#### 3. Error Dialog
- Error Display: ✅ / ❌
- Retry Button: ✅ / ❌
- Recovery: ✅ / ❌
- Styling: ✅ / ❌
- Notes: _________________

#### 4. Infinite Scroll
- Load More Trigger: ✅ / ❌
- Loading Indicator: ✅ / ❌
- Duplicate Prevention: ✅ / ❌
- No Spam Loading: ✅ / ❌
- Notes: _________________

#### Overall
- All Tests: ✅ PASS / ❌ FAIL
- Critical Issues: None / [List issues]
- Ready for Release: ✅ YES / ❌ NO
```

---

**Created**: 2025-10-30  
**Status**: Ready for QA Testing  
**Priority**: High (affects core HomeScreen functionality)
