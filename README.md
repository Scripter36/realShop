# realShop
유저들의 수요와 공급에 따라 아이템의 가격이 달라지는 상점 플러그인입니다.
## 가격 책정법
`가치`: 변하지 않는 아이템의 고유 가치 값입니다.  
`개수`: 아이템이 시장에 풀려 있는 개수입니다.  
`가격`: 아이템을 살 때 필요한 돈입니다.  
아이템 한 개의 가격은 `(가치 / 개수 + 가치 / 나중개수)/2` 로 계산합니다.  
예) 가치 100000, 개수 10일 때 구매시 (100000/10 + 100000/9)/2 = 약 10555원  
아이템을 여러 개 살 경우 위 계산을 반복하여 합산합니다.  
## 명령어
 `/rs buy <amount>` - 상점에서 선택한 아이템을 <수량> 개 구매합니다.  
 `/rs 구매 <수량>`

 `/rs sell <amount>` - 상점에서 선택한 아이템을 <수량> 개 판매합니다.  
 `/rs 판매 <수량>`

 `/rs instantbuy <name> <amount>` - <이름> 을 <수량> 개 즉시 구매합니다.  
 `/rs 즉시구매 <이름> <수량>`

 `/rs instantsell <name> <amount>` - <이름> 을 <수량> 개 즉시 판매합니다.  
 `/rs 즉시판매 <이름> <수량>`

 `/rs price <name> <amount>` - <이름> 을 <수량> 개 샀을 때의 가격을 알려줍니다. 수량이 음수면 판매  
 `/rs 가격 <이름> <수량>`

 `/rs worth <name>` - <이름> 의 가치를 알려줍니다.  
 `/rs 가치 <이름>`

 `/rs amount <name>` - <이름> 의 개수를 알려줍니다.  
 `/rs 수량 <이름>`

 `/rs setworth <name> <worth>` - <이름> 의 가치를 <가치> 로 설정합니다.  
 `/rs 가치설정 <이름> <가치>`

 `/rs setamount <name> <amount>` - <이름> 의 수량을 <수량> 으로 설정합니다.  
 `/rs 수량설정 <이름> <수량>`

 `/rs shop create <name> [buy] [sell]` - 터치로 <이름> 을 파는 상점을 생성합니다. [buy], [sell] 은 true  
 `/rs 상점 생성 <이름> [구매] [판매]` - 또는 false를 써야 하며, 구매, 판매 가능 여부입니다. (On/Off)  

 `/rs shop remove` - 터치로 상점을 제거합니다.  
 `/rs 상점 제거`

 `/rs additem <name> [worth] [amount]` - 손에 든 아이템을 <이름> 으로 등록합니다. 가치, 수량 설정 가능  
 `/rs 아이템추가 <이름> [가치] [수량]` - 가치와 수량의 기본값은 100000, 1입니다.  

 `/rs removeitem <name>` - <이름>을 등록 해제합니다.  
 `/rs 아이템제거 <이름>`

 `/rs itemlist` - 등록된 아이템 리스트를 보여줍니다.  
 `/rs 아이템목록`
## 퍼미션
permissions:  
  realshop.commands.*:  
    description: Allows you to use all realShop commands  
    children:  
      realshop.commands.changeworth: true  
      realshop.commands.changeamount: true  
      realshop.commands.getprice: true  
      realshop.commands.getworth: true  
      realshop.commands.getamount: true  
      realshop.commands.buy: true  
      realshop.commands.sell: true  
      realshop.commands.instantbuy: true  
      realshop.commands.instantsell: true  
      realshop.commands.createshop: true  
      realshop.commands.removeshop: true  
      realshop.commands.additem: true  
      realshop.commands.removeitem: true  
      realshop.commands.itemlist: true  
  realshop.commands.changeworth:  
    description: Allows you to change a worth of item  
    default: OP  
  realshop.commands.changeamount:  
    description: Allows you to change a amount of item  
    default: OP  
  realshop.commands.getprice:  
    description: Allows you to get a price of item  
    default: true  
  realshop.commands.getworth:  
    description: Allows you to get a worth of item  
    default: true  
  realshop.commands.getamount:  
    description: Allows you to get a amount of item  
    default: true  
  realshop.commands.buy:  
    description: Allows you to buy some items  
    default: true  
  realshop.commands.sell:  
    description: Allows you to sell some items  
    default: true  
  realshop.commands.instantbuy:  
    description: Allows you to buy some items instantly  
    default: OP  
  realshop.commands.instantsell:  
    description: Allows you to sell some items instantly  
    default: OP  
  realshop.commands.createshop:  
    description: Allows you to create a shop  
    default: OP  
  realshop.commands.additem:  
    description: Allows you to register a item  
    default: OP  
  realshop.commands.removeshop:  
    description: Allows you to remove a shop  
    default: OP  
  realshop.commands.removeitem:  
    description: Allows you to remove a item  
    default: OP  
  realshop.commands.itemlist:  
    description: Shows item list  
    default: OP  
  
 전부 커맨드 퍼미션이고, 커맨드 이름과 직결됩니다.
## 예상되는 효과
- 소외받는 생산직이 줄어듭니다.  
여러 서버에 들어가 보면 어떤 농사가 좋다 같은 말이 있는데, 이 플러그인을 적용할 경우 결국 가격이 다 비슷해집니다.
- 사재기를 방지합니다.  
아이템을 사면 살 수록 가격이 눈덩이처럼 불어나기 때문에, 사재기가 힘들어집니다.  
## 한계점
- 수요, 또는 공급이 매우 적거나 없는 아이템은 사용하기 힘듭니다.  
각종 농작물과 해산물 등 수요가 매우 적거나, 기반암 등 공급이 없는 아이템의 경우 경제가 파탄납니다.  
이 경우 무한상점인 다른 플러그인을 사용하셔야 합니다.