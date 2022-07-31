# Banking-With-Security

## Deadline 30/07/2022 Cumartesi 23:59

# BANKING HOMEWORK WITH SECURITY

Arkadaşlar bu ödevde son yaptığımız bankacılık ödevine güvenlik ve user yönetim katmanı ekleyeceğiz.

### !! NOT 1 !!
Arkadaşlar kodunuza **local anestezi** yapacaksınız. Zaten çalışan kodun biraz önüne biraz arkasına security katmanı ekleyeceksiniz. Sıfırdan yazmayın servislerinizi sakın!!!

### !! NOT 2 !!
Arkadaşlar custom userDetailsService'i benim dökümanımda almak bir seçenek ancak myBatis ile yapmakta bir seçenek. myBatis ile yapanlar ekstra puan alacaktır. Zor değil myBatis'e bir query koyacaksınız loadByUsername methodunda myBatis'teki query'inizi çağıracaksınız!

1 - Sistemimizde userlar olacaktır artık. Userlarımızın
```
{
    id,
    username,
    password,
    authorities
}
```
şeklinde propertyleri ve database tarafından bakınca kolonları olacaktır. Authorities kolonu virgüllerle ayrılmış(varchar olacak) bir şekilde yetkileri tutacaktır. Buraya şimdilik elle bir kaç user eklemenizi bekliyorum. (register api'si yazmanızı gerek yok kendiniz elle ekleyin)

2 - Sistemimizde authorities olarak

- CREATE_ACCOUNT
- REMOVE_ACCOUNT

şeklinde iki tane yetkimiz olacaktır. Bu yetkileri eklediğiniz userlara kafanıza göre verebilirsiniz. Size bırakıyorum. Ancak en azından bir user'da **REMOVE_ACCOUNT** yetkisi olsun.

3 - Sistemimiz güvenliği **JWT** authentication ile sağlayacaktır. Yani **/auth** URL'i dışında hiçbir yere authenticate olmadan erişemeyeceğiz!
- Kullanıcının username ve password ile giriş yapabileceği /auth URL'ine sahip bir endpoint(action) olmalıdır.
- Kullanıcının bilgileri database'dan alınacaktır. (Custom user service yazıyorduk hatırlayın!)
- User database'den alındıgından authoritiesleri'de alınıp düzgünce user nesnesinin içine koyulmalıdır.


4 - Sistemimizdeki accountlar artık user ile ilişkili olacaktır.(accounts tablosuna user_id şeklinde foreign key eklenebilir).

4.1 - Account create işlemi yetki olarak **CREATE_ACCOUNT** yetkisi gerektirecektir. Buna göre **@Configuration** sınıfında **configure()** methodunda uygun bir şekilde tanımları yapmanızı istiyorum. **antmatchers() methodunun override edilmiş halinde Http methodunu'da belirleyebilmektesiniz.**

```
    @Override
	protected void configure(HttpSecurity http) throws Exception {
        http
    	.authorizeHttpRequests()
    	.antMatchers(HttpMethod.POST, "/accounts/**").hasAuthority("CREATE_ACCOUNT");
    	....
    	....
    	....
	}
```
Ve create işlemi yapılırken authenticated user'in id'si alınıp accounts tablosuna user_id olarak yazılacak.

4.2 Artık herkes kendi hesap detayını okuyacak. Login olan kullanıcı account detayını okuma isteği atarken kendine ait olmayan bir account'u görmeye çalışıyor ise
**HTTP 403**
```
{
    message : "Invalid Account Number[veya id ile bakıyorsanız id]" 
}
```
şeklinde cevap dönmenizi istiyorum.

**Burada şu şekilde birşey yapılabilir. Gelen account numarasına göre account detayını getir, sonra account'ın kolonunda yazan user_id ile authenticate olmuş user'ı karşılaştır.**

4.3 Yine aynı şekilde herkes kendi hesabına para yatırabilir veya kendi hesabından para transferi yapabilir. Bu endpointlerde de eğer başka account üstünde işlem yapmaya çalışıyor ise kullanıcı yukarıdaki mesajın aynısını dönmenizi istiyorum.

4.4 Account silme işlemini sadece **REMOVE_ACCOUNT** yetkisine sahip kullanıcılar yapabilmelidir. Bunun için **Configuration** sınıfına

```
    @Override
	protected void configure(HttpSecurity http) throws Exception {
        http
    	.authorizeHttpRequests()
    	.antMatchers(HttpMethod.DELETE, "/accounts/**").hasAuthority("REMOVE_ACCOUNT");
    	.....
    	.....
    	.....
   }
```

şeklinde ekleme yaparak çözmenizi istiyorum.

Bunlar dışında diğer servislere şimdilik karışmanızı istemiyorum. (Zaten logları getiren servis kaldı geriye, ona birşey yapmanıza gerek yok)
    		


