
1. Deploy MailClient/pinternals_MailClientAdapter.ear 
2. Create namespace 'urn:pinternals-adapters' at any non-local SWCV
3. Create AdapterMetadata MailClientAdapter at 'urn:pinternals-adapters' and import /MailClientRA/src/com/pinternals/mailclientadapter/MailClientAdapter.xml there
4. Activate
5. Create CC of {urn:pinternals-adapters}MailClientAdapter and make some common stuff (SA/RA or ICo).
6. Locate CC at pimon
