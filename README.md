# Paralel_Programlama - Odev-2
Çokgen Dışbükeylik Kontrol Uygulaması
Bu Java Swing tabanlı masaüstü uygulaması, kullanıcıların etkileşimli bir şekilde çokgenler çizmesini ve bu çokgenlerin dışbükey (convex) veya içbükey (concave) olup olmadığını hem seri (tek işlemcili) hem de paralel (çok işlemcili) algoritmalarla kontrol etmesini sağlar. Uygulama, algoritmaların performans farklarını mikrosaniye (µs) cinsinden görsel olarak sunar.

Özellikler
Etkileşimli Çizim: Fare tıklamalarıyla kolayca çokgen oluşturabilirsiniz.

Görsel Geri Bildirim: Çizim sırasında çokgenin kenarlarını görün ve kontrol sonrası sonucun yeşil (dışbükey) veya kırmızı (içbükey) renkle gösterilmesini izleyin.

Seri Kontrol: Çokgenin dışbükeyliğini tek bir işlemci çekirdeği kullanarak kontrol eder.

Paralel Kontrol: Çokgenin dışbükeyliğini, bilgisayarınızın tüm işlemci çekirdeklerini kullanarak, daha büyük veri setlerinde önemli performans avantajı sağlayacak şekilde kontrol eder.

Performans Karşılaştırması: Seri ve paralel algoritmaların çalışma sürelerini mikrosaniye (µs) cinsinden anında görün.

Akıllı Optimizasyon: Çok küçük çokgenler için (örn. 1000 noktadan az), paralel işlemin getirdiği ek yükten (overhead) kaçınmak adına otomatik olarak seri kontrole geçiş yapar.

Rastgele Şekiller: Önceden tanımlanmış dışbükey ve içbükey örnek çokgenleri tek bir tıklamayla yükleyerek hızlıca test yapabilirsiniz.

Temizleme ve Sıfırlama: Çizimi ve sonuçları kolayca temizleyin veya uygulamayı başlangıç durumuna getirin.

Çokgen Dışbükeyliği Nasıl Kontrol Edilir?
Uygulamanın temelinde, bir çokgenin dışbükey olup olmadığını belirleyen bir geometri algoritması yatar. Bu algoritma, çokgenin her köşesindeki dönüş yönünü kontrol eder.

Temel Prensip: Tüm Dönüşler Aynı Yönde Olmalı
Bir çokgenin dışbükey olması için, tüm kenarlarının oluşturduğu açılardaki dönüş yönlerinin (saat yönü veya saat yönünün tersi) hep aynı olması gerekir. Eğer çokgenin içinde herhangi bir "çukur" veya "girinti" varsa, o noktada dönüş yönü değişecektir. İşte bu değişikliği tespit ederek çokgenin dışbükey olup olmadığını anlarız.

"Çapraz Çarpım" Testi
Bilgisayar bu dönüş yönünü anlamak için "çapraz çarpım" denen zekice bir matematiksel testi kullanır:

Üçer Üçer Bakma: Çokgenin üzerindeki her üç komşu noktayı sırayla alırız (örneğin A, B, C noktaları).

Dönüş Yönü Hesaplama: Bilgisayar, bu A, B, C noktalarına bakarak B noktasında C'ye giderken A'dan ne tarafa doğru bir "dönüş" olduğunu hesaplar.

Bu hesaplama sonucunda pozitif bir sayı çıkarsa, genellikle saat yönünün tersine bir dönüş olduğunu gösterir.

Negatif bir sayı çıkarsa, genellikle saat yönünde bir dönüş olduğunu gösterir.

Sıfır çıkarsa, üç noktanın da aynı düz çizgide olduğu anlamına gelir, yani bir dönüş yoktur.

Dışbükeylik Kontrolü:

Uygulama önce çokgenin genel dönüş yönünü (ilk sıfır olmayan çapraz çarpım işaretini) belirler.

Daha sonra, çokgenin her bir köşesini (tüm ardışık üç noktayı) tek tek kontrol eder.

Eğer herhangi bir köşedeki dönüş yönü, genel dönüş yönünden farklı çıkarsa, çokgenin içbükey olduğuna karar verilir ve kontrol hemen durdurulur.

Tüm köşeler aynı dönüş yönünü sergilerse, çokgen dışbükey olarak kabul edilir.

Performans: Tek Başına mı, Ekip Çalışmasıyla mı? (Seri ve Paralel)
Bu kısım, bilgisayarın bir işi yapma hızını karşılaştırdığımız yerdir.

Seri Kontrol:

Nasıl Çalışır? Bilgisayar, çokgenin dışbükeyliğini kontrol etme işini tek bir işlemci çekirdeğiyle baştan sona, adım adım yapar. Tıpkı bir kişinin tek başına tüm evi süpürmesi gibi.

Ne Zaman Hızlıdır? Eğer çokgenin nokta sayısı azsa (örneğin 4 nokta gibi basit bir dörtgen), bu tek kişilik çalışma çok hızlı biter. Çünkü işi paylaştırmak, başkasına görev vermek gibi ek bir "yük" (overhead) olmaz.

Paralel Kontrol:

Nasıl Çalışır? Bilgisayar, çokgenin noktalarını gruplara ayırır ve bu grupları farklı işlemci çekirdeklerine aynı anda işlettirir. Her çekirdek kendi grubunu kontrol eder. Sonra tüm çekirdekler sonuçlarını birleştirir. Tıpkı birden fazla kişinin aynı anda farklı odaları süpürüp, sonra evi temizledik demeleri gibi.

"Böl ve Fethet" Sistemi: Java'nın Fork/Join Framework'ünü kullanırız. Bu sistem, işi otomatik olarak daha küçük parçalara böler ve bilgisayarın boşta duran çekirdeklerine dağıtır.

Akıllı Bölünme (THRESHOLD = 5000): Eğer bir görev çokgenin yeterince büyük bir kısmını içeriyorsa (5000'den fazla noktaya sahipse), kendisini daha küçük alt görevlere böler. Bu eşik değeri, gereksiz yere çok fazla küçük görev oluşturulmasını ve dolayısıyla organizasyon yükünü (overhead) azaltarak performansı artırır.

Otomatik Seri Geçiş (MIN_POINTS_FOR_PARALLEL = 1000): Paralel kontrole başlamadan önce, çokgenin toplam nokta sayısına bakarız. Eğer nokta sayısı 1000'den azsa, paralel işlemin getireceği başlangıç yükünün seri işlemden daha uzun süreceğini bildiğimiz için otomatik olarak seri kontrolü kullanırız. Bu sayede her zaman en iyi performansı elde etmeye çalışırız.

Bu akıllı sistemler sayesinde, eğer çokgeniniz çok fazla noktadan oluşuyorsa (örneğin binlerce), bilgisayarın tüm çekirdeklerini kullanması (paralel kontrol), tek başına yapmasından çok daha hızlı olur. Ama az noktalı bir şekilse, tek başına yapması daha mantıklıdır.

Kurulum ve Çalıştırma
Bu uygulama standart bir Java geliştirme ortamı (JDK) gerektirir. Ek bir kütüphaneye ihtiyacınız yoktur.

Kodu Kaydedin: Sağlanan Java kodunu ConvexityCheckerSwingApp.java adıyla bir dosyaya kaydedin.

Derleme: Komut istemcisini (veya terminali) açın, dosyanın kaydedildiği dizine gidin ve aşağıdaki komutu çalıştırın:

javac ConvexityCheckerSwingApp.java

Çalıştırma: Derleme başarılı olduktan sonra, uygulamayı başlatmak için aşağıdaki komutu kullanın:

java ConvexityCheckerSwingApp

IDE Kullanımı (Önerilir)
Eğer bir Entegre Geliştirme Ortamı (IDE) kullanıyorsanız (örneğin IntelliJ IDEA, Eclipse, VS Code):

Yeni bir standart Java projesi oluşturun.

ConvexityCheckerSwingApp.java dosyasını projenizin src klasörüne (veya uygun bir paket altına) kopyalayın.

ConvexityCheckerSwingApp sınıfındaki main metodunu çalıştırın.

Kullanım
Uygulama açıldığında koyu gri bir çizim alanı göreceksiniz.

Çokgen Çizme: Çizim alanına fare ile tıklayarak çokgeninizin köşelerini belirleyin.

Çizimi Bitirme: Yeterli sayıda nokta (en az 3) ekledikten sonra Çokgeni Bitir butonuna tıklayın. Çokgen kapanacak ve çizim modu sona erecektir.

Kontrol Etme:

Seri Kontrol butonuna tıklayarak çokgeni tek işlemcili algoritmamızla kontrol edin.

Paralel Kontrol butonuna tıklayarak çokgeni çok işlemcili algoritmamızla kontrol edin.

Sonuç ve geçen süreler (mikrosaniye cinsinden) üstteki etiketlerde görünecektir.

Rastgele Şekil Ekleme: Kendi çokgeninizi çizmek istemiyorsanız, Rastgele Çokgen Ekle butonuna tıklayarak önceden tanımlanmış dışbükey veya içbükey bir şekli ekrana getirebilirsiniz. Her tıklamada farklı bir şekil yüklenecektir.

Temizleme: Temizle butonuna basarak mevcut çokgeni ve sonuçları silebilir, yeni bir çizime başlayabilirsiniz.
