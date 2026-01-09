package com.toplanti.dashboard.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LanguageManager {

    private static LanguageManager instance;
    private Locale currentLocale;
    private final Map<String, Map<String, String>> translations;

    private LanguageManager() {
        this.translations = new HashMap<>();
        this.currentLocale = new Locale("tr", "TR");
        loadTranslations();
    }

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    private void loadTranslations() {
        Map<String, String> tr = new HashMap<>();
        Map<String, String> en = new HashMap<>();

        
        tr.put("app.title", "Toplantı Özetleyici Dashboard");
        en.put("app.title", "Meeting Summarizer Dashboard");

        
        tr.put("login.title", "Giriş Yap");
        en.put("login.title", "Login");
        tr.put("login.username", "Kullanıcı Adı");
        en.put("login.username", "Username");
        tr.put("login.password", "Şifre");
        en.put("login.password", "Password");
        tr.put("login.button", "Giriş");
        en.put("login.button", "Sign In");
        tr.put("login.register", "Hesabınız yok mu? Kayıt olun");
        en.put("login.register", "Don't have an account? Register");
        tr.put("login.forgot", "Şifremi Unuttum");
        en.put("login.forgot", "Forgot Password");
        tr.put("login.error", "Kullanıcı adı veya şifre hatalı");
        en.put("login.error", "Invalid username or password");
        tr.put("login.success", "Giriş başarılı");
        en.put("login.success", "Login successful");

        
        tr.put("register.title", "Kayıt Ol");
        en.put("register.title", "Register");
        tr.put("register.fullname", "Ad Soyad");
        en.put("register.fullname", "Full Name");
        tr.put("register.email", "E-posta");
        en.put("register.email", "Email");
        tr.put("register.department", "Departman");
        en.put("register.department", "Department");
        tr.put("register.button", "Kayıt Ol");
        en.put("register.button", "Sign Up");
        tr.put("register.login", "Zaten hesabınız var mı? Giriş yapın");
        en.put("register.login", "Already have an account? Login");
        tr.put("register.success", "Kayıt başarılı");
        en.put("register.success", "Registration successful");

        
        tr.put("dashboard.title", "Dashboard");
        en.put("dashboard.title", "Dashboard");
        tr.put("dashboard.stats", "{0} toplantı | {1} görev");
        en.put("dashboard.stats", "{0} meetings | {1} tasks");
        tr.put("dashboard.loading", "Yükleniyor...");
        en.put("dashboard.loading", "Loading...");
        tr.put("dashboard.connected", "Bağlı");
        en.put("dashboard.connected", "Connected");
        tr.put("dashboard.disconnected", "Bağlantı Kesildi");
        en.put("dashboard.disconnected", "Disconnected");
        tr.put("dashboard.connecting", "Bağlanıyor...");
        en.put("dashboard.connecting", "Connecting...");

        
        tr.put("tasks.title", "Görevlerim");
        en.put("tasks.title", "My Tasks");
        tr.put("tasks.assigned", "Atanan Görevler");
        en.put("tasks.assigned", "Assigned Tasks");
        tr.put("tasks.created", "Oluşturduğum Görevler");
        en.put("tasks.created", "Created Tasks");
        tr.put("tasks.empty", "Henüz görev bulunmuyor");
        en.put("tasks.empty", "No tasks yet");
        tr.put("tasks.complete", "Tamamla");
        en.put("tasks.complete", "Complete");
        tr.put("tasks.delete", "Sil");
        en.put("tasks.delete", "Delete");
        tr.put("tasks.edit", "Düzenle");
        en.put("tasks.edit", "Edit");
        tr.put("tasks.assign", "Görev Ata");
        en.put("tasks.assign", "Assign Task");
        tr.put("tasks.assignee", "Atanan Kişi");
        en.put("tasks.assignee", "Assignee");
        tr.put("tasks.duedate", "Son Tarih");
        en.put("tasks.duedate", "Due Date");

        
        tr.put("meetings.title", "Toplantı Özetleri");
        en.put("meetings.title", "Meeting Summaries");
        tr.put("meetings.empty", "Henüz toplantı bulunmuyor");
        en.put("meetings.empty", "No meetings yet");
        tr.put("meetings.refresh", "Yenile");
        en.put("meetings.refresh", "Refresh");
        tr.put("meetings.filter.all", "Tümü");
        en.put("meetings.filter.all", "All");
        tr.put("meetings.filter.platform", "Platform:");
        en.put("meetings.filter.platform", "Platform:");
        tr.put("meetings.participants", "{0} katılımcı");
        en.put("meetings.participants", "{0} participants");
        tr.put("meetings.keypoints", "{0} nokta");
        en.put("meetings.keypoints", "{0} points");
        tr.put("meetings.dateunknown", "Tarih bilinmiyor");
        en.put("meetings.dateunknown", "Date unknown");

        
        tr.put("detail.title", "Toplantı Detayı");
        en.put("detail.title", "Meeting Detail");
        tr.put("detail.summary", "Özet");
        en.put("detail.summary", "Summary");
        tr.put("detail.transcription", "Transkript");
        en.put("detail.transcription", "Transcription");
        tr.put("detail.tasks", "Görevler");
        en.put("detail.tasks", "Tasks");
        tr.put("detail.keypoints", "Önemli Noktalar:");
        en.put("detail.keypoints", "Key Points:");
        tr.put("detail.participants.title", "Katılımcılar:");
        en.put("detail.participants.title", "Participants:");
        tr.put("detail.summary.notfound", "Özet bulunamadı");
        en.put("detail.summary.notfound", "Summary not found");
        tr.put("detail.transcription.notfound", "Transkript bulunamadı");
        en.put("detail.transcription.notfound", "Transcription not found");
        tr.put("detail.tasks.empty", "Bu toplantıda görev bulunmuyor");
        en.put("detail.tasks.empty", "No tasks in this meeting");
        tr.put("detail.export", "Dışa Aktar");
        en.put("detail.export", "Export");
        tr.put("detail.export.pdf", "PDF olarak kaydet");
        en.put("detail.export.pdf", "Save as PDF");
        tr.put("detail.export.word", "Word olarak kaydet");
        en.put("detail.export.word", "Save as Word");

        
        tr.put("priority.critical", "Kritik");
        en.put("priority.critical", "Critical");
        tr.put("priority.high", "Yüksek");
        en.put("priority.high", "High");
        tr.put("priority.medium", "Orta");
        en.put("priority.medium", "Medium");
        tr.put("priority.low", "Düşük");
        en.put("priority.low", "Low");

        
        tr.put("status.pending", "Beklemede");
        en.put("status.pending", "Pending");
        tr.put("status.inprogress", "Devam Ediyor");
        en.put("status.inprogress", "In Progress");
        tr.put("status.review", "İncelemede");
        en.put("status.review", "In Review");
        tr.put("status.completed", "Tamamlandı");
        en.put("status.completed", "Completed");
        tr.put("status.cancelled", "İptal Edildi");
        en.put("status.cancelled", "Cancelled");

        
        tr.put("role.ceo", "CEO");
        en.put("role.ceo", "CEO");
        tr.put("role.director", "Direktör");
        en.put("role.director", "Director");
        tr.put("role.manager", "Yönetici");
        en.put("role.manager", "Manager");
        tr.put("role.teamlead", "Takım Lideri");
        en.put("role.teamlead", "Team Lead");
        tr.put("role.senior", "Kıdemli Çalışan");
        en.put("role.senior", "Senior Employee");
        tr.put("role.employee", "Çalışan");
        en.put("role.employee", "Employee");
        tr.put("role.intern", "Stajyer");
        en.put("role.intern", "Intern");

        
        tr.put("settings.title", "Ayarlar");
        en.put("settings.title", "Settings");
        tr.put("settings.language", "Dil");
        en.put("settings.language", "Language");
        tr.put("settings.theme", "Tema");
        en.put("settings.theme", "Theme");
        tr.put("settings.theme.light", "Açık Tema");
        en.put("settings.theme.light", "Light Theme");
        tr.put("settings.theme.dark", "Koyu Tema");
        en.put("settings.theme.dark", "Dark Theme");
        tr.put("settings.notifications", "Bildirimler");
        en.put("settings.notifications", "Notifications");
        tr.put("settings.notifications.enabled", "Bildirimleri Aç");
        en.put("settings.notifications.enabled", "Enable Notifications");
        tr.put("settings.logout", "Çıkış Yap");
        en.put("settings.logout", "Logout");

        
        tr.put("notification.newtask", "Yeni görev atandı");
        en.put("notification.newtask", "New task assigned");
        tr.put("notification.newmeeting", "Yeni toplantı özeti");
        en.put("notification.newmeeting", "New meeting summary");
        tr.put("notification.taskcompleted", "Görev tamamlandı");
        en.put("notification.taskcompleted", "Task completed");
        tr.put("notification.taskdue", "Görev süresi dolmak üzere");
        en.put("notification.taskdue", "Task due soon");

        
        tr.put("search.placeholder", "Ara...");
        en.put("search.placeholder", "Search...");
        tr.put("search.noresults", "Sonuç bulunamadı");
        en.put("search.noresults", "No results found");
        tr.put("search.filter", "Filtrele");
        en.put("search.filter", "Filter");
        tr.put("search.filter.date", "Tarih Aralığı");
        en.put("search.filter.date", "Date Range");
        tr.put("search.filter.platform", "Platform");
        en.put("search.filter.platform", "Platform");
        tr.put("search.filter.priority", "Öncelik");
        en.put("search.filter.priority", "Priority");
        tr.put("search.filter.status", "Durum");
        en.put("search.filter.status", "Status");

        
        tr.put("error.title", "Hata");
        en.put("error.title", "Error");
        tr.put("error.connection", "Bağlantı hatası oluştu");
        en.put("error.connection", "Connection error occurred");
        tr.put("error.loading", "Yüklenirken hata oluştu");
        en.put("error.loading", "Error while loading");
        tr.put("error.permission", "Bu işlem için yetkiniz yok");
        en.put("error.permission", "You don't have permission for this action");
        tr.put("error.validation", "Lütfen tüm alanları doldurun");
        en.put("error.validation", "Please fill in all fields");

        
        tr.put("success.title", "Başarılı");
        en.put("success.title", "Success");
        tr.put("success.saved", "Kaydedildi");
        en.put("success.saved", "Saved");
        tr.put("success.deleted", "Silindi");
        en.put("success.deleted", "Deleted");
        tr.put("success.exported", "Dışa aktarıldı");
        en.put("success.exported", "Exported");

        
        tr.put("confirm.title", "Onay");
        en.put("confirm.title", "Confirmation");
        tr.put("confirm.delete", "Silmek istediğinize emin misiniz?");
        en.put("confirm.delete", "Are you sure you want to delete?");
        tr.put("confirm.logout", "Çıkış yapmak istediğinize emin misiniz?");
        en.put("confirm.logout", "Are you sure you want to logout?");
        tr.put("confirm.yes", "Evet");
        en.put("confirm.yes", "Yes");
        tr.put("confirm.no", "Hayır");
        en.put("confirm.no", "No");
        tr.put("confirm.cancel", "İptal");
        en.put("confirm.cancel", "Cancel");

        
        tr.put("shortcut.search", "Ctrl+F: Ara");
        en.put("shortcut.search", "Ctrl+F: Search");
        tr.put("shortcut.refresh", "F5: Yenile");
        en.put("shortcut.refresh", "F5: Refresh");
        tr.put("shortcut.newtask", "Ctrl+N: Yeni Görev");
        en.put("shortcut.newtask", "Ctrl+N: New Task");
        tr.put("shortcut.settings", "Ctrl+,: Ayarlar");
        en.put("shortcut.settings", "Ctrl+,: Settings");
        tr.put("shortcut.help", "F1: Yardım");
        en.put("shortcut.help", "F1: Help");

        translations.put("tr", tr);
        translations.put("en", en);
    }

    public String get(String key) {
        String lang = currentLocale.getLanguage();
        Map<String, String> langMap = translations.getOrDefault(lang, translations.get("tr"));
        return langMap.getOrDefault(key, key);
    }

    public String get(String key, Object... args) {
        String template = get(key);
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return template;
    }

    public void setLocale(Locale locale) {
        this.currentLocale = locale;
    }

    public void setLanguage(String languageCode) {
        this.currentLocale = new Locale(languageCode);
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public boolean isTurkish() {
        return "tr".equals(currentLocale.getLanguage());
    }

    public boolean isEnglish() {
        return "en".equals(currentLocale.getLanguage());
    }
}

