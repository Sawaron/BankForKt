//package com.shoma.controller;
//
//
//import com.shoma.enity.BankAccount;
//import com.shoma.exceptions.BankTerminalException;
//import com.shoma.repository.BankAccountRepository;
//import com.shoma.service.BankService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.util.Optional;
//import java.util.Scanner;
//
//@Component
//public class BankConsoleController implements CommandLineRunner {
//
//    private final BankService bankService;
//
//    @Autowired
//    private BankAccountRepository bankAccountRepository;
//
//
//    @Autowired
//    public BankConsoleController(BankService bankService) {
//        this.bankService = bankService;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        Scanner input = new Scanner(System.in);
//        while (true) {
//            System.out.println("1-Создать карту");
//            System.out.println("2-Проверить баланс");
//            System.out.println("3-Пополнить баланс");
//            System.out.println("4-Снять");
//            System.out.println("5-Вывести всех пользователей");
//            System.out.println("6-Выход");
//
//            int choice = input.nextInt();
//            input.nextLine();
//
//            switch (choice) {
//                case 1:
//                    String generatedNumber;
//                    while (true) {
//                        generatedNumber = bankService.generateCardNumber();
//                        System.out.println("Сгенерированный номер: " + generatedNumber);
//                        System.out.println("Вас устраивает этот номер (да/нет)");
//
//                        if (input.nextLine().equalsIgnoreCase("да")) break;
//                    }
//                    System.out.println("Введите ПИН-код (22-МАКС):");
//                    String pin = input.nextLine();
//                    System.out.println("Введите ваше имя: ");
//                    String ownerName = input.nextLine();
//
//
//                    bankService.saveAccount(generatedNumber, pin, ownerName);
//                    System.out.println("Карта успешно создана!");
//                    break;
//                case 2:
//                    try{
//                        System.out.println("Введите номер карты:");
//                        String cardNumber = input.nextLine();
//
//                        System.out.println("Введите пин-код");
//                        String pincode = input.nextLine();
//
//                        BigDecimal balance  = bankService.checkBalance(cardNumber, pincode);
//                        System.out.println("Ваш баланс: " +balance+ "$");
//
//                        input.nextLine();
//
//                    }catch(BankTerminalException e){
//                        System.out.println("Ошибка: " + e.getMessage());
//
//                    }catch (Exception e){
//                        System.out.println("Системная ошибка!!");
//                    }
//                    break;
//
//                case 3:
//                    int failCardNumber = 3;
//                    outerloop:
//                    while (true) {
//                        System.out.println("Введите номер карты");
//                        String cardNumber = input.nextLine();
//                        Optional<BankAccount> bankAccount = bankAccountRepository.findByCardNumber(cardNumber);
//                        if (!bankAccount.isPresent()) {
//                            System.out.println("Такой карты не существует");
//                            failCardNumber--;
//                            if (failCardNumber == 0) {
//                                System.out.println("Максимальное количество попыток для входа в аккаунт!");
//                                break;
//                            }
//                            continue;
//                        }
//
//                        while (true) {
//                            System.out.println("Введите сумму");
//                            BigDecimal money = input.nextBigDecimal();
//                            if (money.compareTo(BigDecimal.ZERO) < 0) {
//                                System.out.println("Сумма не может быть отрицательной!");
//
//                            }
//                            BankAccount account = bankAccount.get();
//
//                            BigDecimal moneyInto = account.getBalance();
//                            BigDecimal sum = money.add(moneyInto);
//                            account.setBalance(sum);
//                            bankAccountRepository.save(account);
//                            System.out.println("Ваш баланс: " + bankService.checkBalance(cardNumber, account.getPinCode()) + "$");
//                            input.nextLine();
//                            break outerloop;
//                        }
//                    }
//                case 4:
//                    failCardNumber = 3;
//                    outerloop:
//                    while (true) {
//                        System.out.println("Введите номер карты");
//                        String cardNumber = input.nextLine();
//                        Optional<BankAccount> bankAccount = bankAccountRepository.findByCardNumber(cardNumber);
//
//                        while (true) {
//                            if (!bankAccount.isPresent()) {
//                                System.out.println("Такой карты не существует");
//                                failCardNumber--;
//                                if (failCardNumber == 0) {
//                                    System.out.println("Максимальное количество попыток для входа в аккаунт!");
//                                    break outerloop;
//                                }
//                                continue;
//                            }
//                            while (true) {
//                                System.out.println("Введите сумму которую хотите снять");
//
//                                BankAccount account = bankAccount.get();
//                                BigDecimal money = input.nextBigDecimal();
//                                BigDecimal moneyInto = account.getBalance();
//                                BigDecimal sum = moneyInto.subtract(money);
//
//                                if (money.compareTo(BigDecimal.ZERO) < 0) {
//                                    System.out.println("Вы не можете снять отрицательную сумму!");
//                                    break outerloop;
//
//                                } else if (sum.compareTo(moneyInto) > 0) {
//                                    System.out.println("Вы не можете снять денег больше чем у вас на счету!");
//
//                                }
//                                account.setBalance(sum);
//                                bankAccountRepository.save(account);
//                                System.out.println("Ваш баланс: " + bankService.checkBalance(cardNumber, account.getPinCode()) + "$");
//                                input.nextLine();
//                                break;
//                            }
//                            break;
//                        }
//                        break;
//
//                    }
//
//                    break;
//                case 5:
//                    for (BankAccount account : bankAccountRepository.findAll()) {
//                        System.out.println(account);
//                    }
//                    input.nextLine();
//                    break;
//                case 6:
//                    break;
//                default:
//                    throw new IllegalStateException("Неизвестное значение: " + choice);
//            }
//        }
//    }
//}
