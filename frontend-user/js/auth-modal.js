/**
 * Auth Modal Component (Đăng Nhập / Đăng Ký)
 * Tương thích với IdentityService của Backend
 */
const AuthModal = {
  currentTab: 'login', // 'login' hoặc 'register'

  init() {
    this.modalEl = document.getElementById('auth-modal');
    this.closeBtn = document.getElementById('close-auth-modal');
    this.tabLogin = document.getElementById('tab-auth-login');
    this.tabRegister = document.getElementById('tab-auth-register');
    this.formLogin = document.getElementById('form-login');
    this.formRegister = document.getElementById('form-register');

    this.closeBtn.addEventListener('click', () => this.close());
    this.modalEl.addEventListener('click', (e) => {
      if (e.target === this.modalEl) this.close();
    });

    this.tabLogin.addEventListener('click', () => this.switchTab('login'));
    this.tabRegister.addEventListener('click', () => this.switchTab('register'));

    this.formLogin.addEventListener('submit', (e) => this.handleLogin(e));
    this.formRegister.addEventListener('submit', (e) => this.handleRegister(e));

    this.checkCurrentSession();
  },

  open(tab = 'login') {
    this.switchTab(tab);
    this.modalEl.classList.add('active');
    document.body.style.overflow = 'hidden';
  },

  close() {
    this.modalEl.classList.remove('active');
    document.body.style.overflow = '';
  },

  switchTab(tab) {
    this.currentTab = tab;
    if (tab === 'login') {
      this.tabLogin.classList.add('active');
      this.tabRegister.classList.remove('active');
      this.formLogin.style.display = 'flex';
      this.formRegister.style.display = 'none';
    } else {
      this.tabRegister.classList.add('active');
      this.tabLogin.classList.remove('active');
      this.formRegister.style.display = 'flex';
      this.formLogin.style.display = 'none';
    }
  },

  async handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;

    const btn = this.formLogin.querySelector('button[type="submit"]');
    btn.textContent = "Đang xử lý...";
    btn.disabled = true;

    try {
      const res = await ApiService.login(username, password);
      App.showToast(`Chào mừng ${res.user ? res.user.fullName : 'bạn'} quay trở lại CGV!`);
      this.updateUserUI(res.user);
      this.close();
    } catch (err) {
      App.showToast(err.message || "Đăng nhập thất bại, vui lòng kiểm tra lại");
    } finally {
      btn.textContent = "Đăng Nhập";
      btn.disabled = false;
    }
  },

  async handleRegister(e) {
    e.preventDefault();
    const fullName = document.getElementById('reg-fullname').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const phone = document.getElementById('reg-phone').value.trim();
    const password = document.getElementById('reg-password').value;

    const btn = this.formRegister.querySelector('button[type="submit"]');
    btn.textContent = "Đang tạo tài khoản...";
    btn.disabled = true;

    try {
      await ApiService.register({ fullName, email, phone, password });
      App.showToast("Đăng ký tài khoản CGV thành công! Vui lòng đăng nhập.");
      this.switchTab('login');
      document.getElementById('login-username').value = phone || email;
    } catch (err) {
      App.showToast(err.message || "Đăng ký thất bại");
    } finally {
      btn.textContent = "Đăng Ký";
      btn.disabled = false;
    }
  },

  checkCurrentSession() {
    const token = localStorage.getItem('cgv_token');
    const userJson = localStorage.getItem('cgv_user');
    if (token && userJson) {
      try {
        const user = JSON.parse(userJson);
        this.updateUserUI(user);
        return;
      } catch (e) {
        localStorage.removeItem('cgv_token');
        localStorage.removeItem('cgv_user');
      }
    }
    this.updateUserUI(null);
  },

  updateUserUI(user) {
    const authLinkContainer = document.getElementById('top-auth-actions');
    if (!authLinkContainer) return;

    if (user) {
      authLinkContainer.innerHTML = `
        <span style="color: #cfa972; font-weight: bold;">⭐ ${user.fullName || user.username} (${user.membershipTier || 'MEMBER'})</span>
        <a href="#" id="btn-logout" style="color: #ff6b6b; margin-left: 8px;">[Đăng xuất]</a>
      `;
      document.getElementById('btn-logout').addEventListener('click', (e) => {
        e.preventDefault();
        localStorage.removeItem('cgv_token');
        localStorage.removeItem('cgv_user');
        App.showToast("Đã đăng xuất tài khoản");
        this.updateUserUI(null);
      });
    } else {
      authLinkContainer.innerHTML = `
        <a href="#" id="btn-open-login">Đăng nhập</a> / 
        <a href="#" id="btn-open-register">Đăng ký</a>
      `;
      document.getElementById('btn-open-login').addEventListener('click', (e) => {
        e.preventDefault();
        this.open('login');
      });
      document.getElementById('btn-open-register').addEventListener('click', (e) => {
        e.preventDefault();
        this.open('register');
      });
    }
  }
};
