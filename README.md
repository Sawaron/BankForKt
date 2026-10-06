![CI](https://github.com/Sawaron/Bank-Terminal/actions/workflows/ci.yml/badge.svg)

int failCardNumber = 3;
outerloop:
while (true) {
System.out.println("Введите номер карты");
String cardNumber = input.nextLine();
Optional<BankAccount> bankAccount = bankAccountRepository.findByCardNumber(cardNumber);
if (!bankAccount.isPresent()) {
System.out.println("Такой карты не существует");
failCardNumber--;
if (failCardNumber == 0) {
System.out.println("Максимальное количество попыток для входа в аккаунт!");
break outerloop;
}

                        } else {
                            BankAccount account = bankAccount.get();
                            if (account.isBlocked()) {
                                System.out.println("Карта заблокирована обратитесь к поддержке");
                                break outerloop;
                            }
                            int failPin = 3;
                            while (true) {

                                System.out.println("Введите пароль");
                                String pincode = input.nextLine();
                                if (pincode.equals(account.getPinCode())) {
                                    System.out.println("Ваш баланс: " + bankService.checkBalance(cardNumber, pincode) + "$");
                                    input.nextLine();
                                    break outerloop;
                                } else {
                                    System.out.println("Пароль введен не верно. Повторите попытку");
                                    failPin--;
                                    if (failPin == 0) {
                                        System.out.println("Превышено количество попыток. КАРТА ЗАБЛОКИРОВАНА!");

                                        account.setBlocked(true);
                                        bankAccountRepository.save(account);
                                        break outerloop;
                                    }
                                }
                            }
                        }
                    }
                    break;git remote add origin https://github.com/Sawaron/Bank-Terminal.git