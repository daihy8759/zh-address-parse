import zhCnNames from './names'
import provinces from './provinces'
import cities from './cities'
import areas from './areas'
import streets from './streets'

const log = (...infos) => {
    if (process.env.NODE_ENV !== 'production') {
        console.log(...infos)
    }
}

const provinceString = JSON.stringify(provinces)
const cityString = JSON.stringify(cities)
const areaString = JSON.stringify(areas)
const streetString = JSON.stringify(streets);

log(provinces.length + cities.length + areas.length)

/**
 * 需要解析的地址，type是解析的方式，默认是正则匹配
 * @param address
 * @param options?：type： 0:正则，1：树查找, textFilter： 清洗的字段
 * @returns {{}|({area: Array, province: Array, phone: string, city: Array, name: string, detail: Array} & {area: (*|string), province: (*|string), city: (*|string), detail: (Array|boolean|string|string)})}
 * @constructor
 */
const AddressParse = (address, options) => {
    const {type = 0, textFilter = [], nameMaxLength = 4} = typeof options === 'object' ? options : (typeof options === 'number' ? {type: options} : {})

    if (!address) {
        return {}
    }

    const parseResult = {
        phone: '',
        province: [],
        city: [],
        area: [],
        street: [],
        detail: [],
        name: '',
    }
    address = cleanAddress(address, textFilter)
    log('清洗后address --->', address)

    // 识别手机号
    const resultPhone = filterPhone(address)
    address = resultPhone.address
    parseResult.phone = resultPhone.phone
    log('获取电话的结果 --->', address)

    const resultCode = filterPostalCode(address)
    address = resultCode.address
    parseResult.postalCode = resultCode.postalCode
    log('获取邮编的结果 --->', address)

    // 地址分割
    const splitAddress = address.split(' ').filter(item => item).map(item => item.trim())
    log('分割地址 --->', splitAddress)

    const d1 = new Date().getTime()

    // 找省市区和详细地址
    splitAddress.forEach((item, index) => {
        // 识别地址
        if (!parseResult.province[0] || !parseResult.city[0] || !parseResult.area[0] || !parseResult.street[0]) {
            // 两个方法都可以解析，正则和树查找
            let parse = {}
            type === 1 && (parse = parseRegion(item, parseResult))
            type === 0 && (parse = parseRegionWithRegexp(item, parseResult))
            const {province, city, area, street, detail} = parse
            parseResult.province = province || []
            parseResult.area = area || []
            parseResult.city = city || []
            parseResult.street = street || []
            parseResult.detail = parseResult.detail.concat(detail || [])
        } else {
            parseResult.detail.push(item)
        }
    })

    log('--->', splitAddress)

    const d2 = new Date().getTime()

    log('解析耗时--->', d2 - d1)

    const province = parseResult.province[0]
    const city = parseResult.city[0]
    const area = parseResult.area[0]
    const street = parseResult.street[0]
    const detail = parseResult.detail

    // 地址都解析完了，姓名应该是在详细地址里面
    if (detail && detail.length > 0) {
        const copyDetail = [...detail]
        copyDetail.sort((a, b) => a.length - b.length)
        log('copyDetail --->', copyDetail)
        // 排序后从最短的开始找名字，没找到的话就看第一个是不是咯
        const index = copyDetail.findIndex(item => judgeFragmentIsName(item, nameMaxLength))
        let name = ''
        if (index !== -1) {
            name = copyDetail[index]
        } else if (copyDetail[0].length <= nameMaxLength && /[\u4E00-\u9FA5]/.test(copyDetail[0])) {
            name = copyDetail[0]
        }

        // 找到了名字就从详细地址里面删除它
        if (name) {
            parseResult.name = name
            detail.splice(detail.findIndex(item => item === name), 1)
        }
    }

    log(JSON.stringify(parseResult))

    return Object.assign(parseResult, {
        provinceCode: (province && province.code) || '',
        province: (province && province.name) || '',
        cityCode:(city && city.code) || '',
        city: (city && city.name) || '',
        areaCode: (area && area.code) || '',
        area: (area && area.name) || '',
        streetCode:(street && street.code) || '',
        street: (street && street.name) || '',
        detail: (detail && detail.length > 0 && detail.join('')) || ''
    })
}

/**
 * 利用正则表达式解析
 * @param fragment
 * @param hasParseResult
 * @returns {{area: (Array|*|string), province: (Array|*|string), city: (Array|*|string|string), detail: (*|Array)}}
 */
const parseRegionWithRegexp = (fragment, hasParseResult) => {
    log('----- 当前使用正则匹配模式 -----')
    let province = hasParseResult.province || [],
        city = hasParseResult.city || [],
        area = hasParseResult.area || [],
        street = hasParseResult.street || [],
        detail = []

    let matchStr = ''
    if (province.length === 0) {
        for (let i = 1; i < fragment.length; i++) {
            const str = fragment.substring(0, i + 1)
            const regexProvince = new RegExp(`\{\"code\":\"[0-9]{1,6}\",\"name\":\"${str}[\u4E00-\u9FA5]*?\"}`, 'g')
            const matchProvince = provinceString.match(regexProvince)
            if (matchProvince) {
                const provinceObj = JSON.parse(matchProvince[0])
                if (matchProvince.length === 1) {
                    province = []
                    matchStr = str
                    province.push(provinceObj)
                }
            } else {
                break
            }
        }

        if (province[0]) {
            fragment = fragment.replace(new RegExp(matchStr, 'g'), '')
        }

    }

    if (city.length === 0) {
        for (let i = 1; i < fragment.length; i++) {
            const str = fragment.substring(0, i + 1)
            const regexCity = new RegExp(`\{\"code\":\"[0-9]{1,6}\",\"name\":\"${str}[\u4E00-\u9FA5]*?\",\"provinceCode\":\"${province[0] ? `${province[0].code}` : '[0-9]{1,6}'}\"\}`, 'g')
            const matchCity = cityString.match(regexCity)
            if (matchCity) {
                const cityObj = JSON.parse(matchCity[0])
                if (matchCity.length === 1) {
                    city = []
                    matchStr = str
                    city.push(cityObj)
                }
            } else {
                break
            }
        }
        if (city[0]) {
            const {provinceCode} = city[0]
            fragment = fragment.replace(new RegExp(matchStr, 'g'), '')
            if (province.length === 0) {
                const regexProvince = new RegExp(`\{\"code\":\"${provinceCode}\",\"name\":\"[\u4E00-\u9FA5]+?\"}`, 'g')
                const matchProvince = provinceString.match(regexProvince)
                province.push(JSON.parse(matchProvince[0]))
            }
        }

    }

    if (area.length === 0) {
        for (let i = 1; i < fragment.length; i++) {
            const str = fragment.substring(0, i + 1)
            const regexArea = new RegExp(`\{\"code\":\"[0-9]{1,6}\",\"name\":\"${str}[\u4E00-\u9FA5]*?\",\"cityCode\":\"${city[0] ? city[0].code : '[0-9]{1,6}'}\",\"provinceCode\":\"${province[0] ? `${province[0].code}` : '[0-9]{1,6}'}\"\}`, 'g')
            const matchArea = areaString.match(regexArea)
            if (matchArea) {
                const areaObj = JSON.parse(matchArea[0])
                if (matchArea.length === 1) {
                    area = []
                    matchStr = str
                    area.push(areaObj)
                }
            } else {
                break
            }
        }
        if (area[0]) {
            const {provinceCode, cityCode} = area[0]
            fragment = fragment.replace(matchStr, '')
            if (province.length === 0) {
                const regexProvince = new RegExp(`\{\"code\":\"${provinceCode}\",\"name\":\"[\u4E00-\u9FA5]+?\"}`, 'g')
                const matchProvince = provinceString.match(regexProvince)
                province.push(JSON.parse(matchProvince[0]))
            }
            if (city.length === 0) {
                const regexCity = new RegExp(`\{\"code\":\"${cityCode}\",\"name\":\"[\u4E00-\u9FA5]+?\",\"provinceCode\":\"${provinceCode}\"\}`, 'g')
                const matchCity = cityString.match(regexCity)
                city.push(JSON.parse(matchCity[0]))
            }
        }
    }

    if (street.length === 0) {
        for (let i = 1; i < fragment.length; i++) {
            const str = fragment.substring(0, i + 1)
            const regexStreet = new RegExp(`\{\"code\":\"[0-9]{1,9}\",\"name\":\"${str}[\u4E00-\u9FA5]*?\",\"areaCode\":\"${area[0] ? area[0].code : '[0-9]{1,6}'}\",\"cityCode\":\"${city[0] ? city[0].code : '[0-9]{1,6}'}\",\"provinceCode\":\"${province[0] ? `${province[0].code}` : '[0-9]{1,6}'}\"\}`, 'g')
            const matchStreet = streetString.match(regexStreet)
            if (matchStreet) {
                const streetObj = JSON.parse(matchStreet[0])
                if (matchStreet.length === 1) {
                    street = []
                    matchStr = str
                    street.push(streetObj)
                }
            } else {
                break
            }
        }
        if (street[0]) {
            const {provinceCode, cityCode, areaCode} = street[0]
            fragment = fragment.replace(matchStr, '')
            if (province.length === 0) {
                const regexProvince = new RegExp(`\{\"code\":\"${provinceCode}\",\"name\":\"[\u4E00-\u9FA5]+?\"}`, 'g')
                const matchProvince = provinceString.match(regexProvince)
                province.push(JSON.parse(matchProvince[0]))
            }
            if (city.length === 0) {
                const regexCity = new RegExp(`\{\"code\":\"${cityCode}\",\"name\":\"[\u4E00-\u9FA5]+?\",\"provinceCode\":\"${provinceCode}\"\}`, 'g')
                const matchCity = cityString.match(regexCity)
                city.push(JSON.parse(matchCity[0]))
            }
            if (area.length === 0) {
                const regexArea = new RegExp(`\{\"code\":\"${areaCode}\",\"name\":\"[\u4E00-\u9FA5]+?\",\"cityCode\":\"${cityCode}\",\"provinceCode\":\"${provinceCode}\"\}`, 'g')
                const matchArea = areaString.match(regexArea)
                if(matchArea) {
                    area.push(JSON.parse(matchArea[0]))
                }
            }
        }
    }


    // 解析完省市区如果还存在地址，则默认为详细地址
    if (fragment.length > 0) {
        detail.push(fragment)
    }

    return {
        province,
        city,
        area,
        street,
        detail,
    }
}

/**
 * 利用树向下查找解析
 * @param fragment
 * @param hasParseResult
 * @returns {{area: Array, province: Array, city: Array, detail: Array}}
 */
const parseRegion = (fragment, hasParseResult) => {
    log('----- 当前使用树查找模式 -----')
    let province = [], city = [], area = [], street = [], detail = []

    if (hasParseResult.province[0]) {
        province = hasParseResult.province
    } else {
        // 从省开始查找
        for (const tempProvince of provinces) {
            const {name} = tempProvince
            let replaceName = ''
            for (let i = name.length; i > 1; i--) {
                const temp = name.substring(0, i)
                if (fragment.indexOf(temp) === 0) {
                    replaceName = temp
                    break
                }
            }
            if (replaceName) {
                province.push(tempProvince)
                fragment = fragment.replace(new RegExp(replaceName, 'g'), '')
                break
            }
        }
    }
    if (hasParseResult.city[0]) {
        city = hasParseResult.city
    } else {
        // 从市区开始查找
        for (const tempCity of cities) {
            const {name, provinceCode} = tempCity
            const currentProvince = province[0]
            // 有省
            if (currentProvince) {
                if (currentProvince.code === provinceCode) {
                    let replaceName = ''
                    for (let i = name.length; i > 1; i--) {
                        const temp = name.substring(0, i)
                        if (fragment.indexOf(temp) === 0) {
                            replaceName = temp
                            break
                        }
                    }
                    if (replaceName) {
                        city.push(tempCity)
                        fragment = fragment.replace(new RegExp(replaceName, 'g'), '')
                        break
                    }
                }
            } else {
                // 没有省，市不可能重名
                for (let i = name.length; i > 1; i--) {
                    const replaceName = name.substring(0, i)
                    if (fragment.indexOf(replaceName) === 0) {
                        city.push(tempCity)
                        province.push(provinces.find(item => item.code === provinceCode))
                        fragment = fragment.replace(replaceName, '')
                        break
                    }
                }
                if (city.length > 0) {
                    break
                }
            }
        }
    }

    // 从区市县开始查找
    for (const tempArea of areas) {
        const {name, provinceCode, cityCode} = tempArea
        const currentProvince = province[0]
        const currentCity = city[0]

        // 有省或者市
        if (currentProvince || currentCity) {
            if ((currentProvince && currentProvince.code === provinceCode)
                || (currentCity && currentCity.code) === cityCode) {
                let replaceName = ''
                for (let i = name.length; i > 1; i--) {
                    const temp = name.substring(0, i)
                    if (fragment.indexOf(temp) === 0) {
                        replaceName = temp
                        break
                    }
                }
                if (replaceName) {
                    area.push(tempArea)
                    !currentCity && city.push(cities.find(item => item.code === cityCode))
                    !currentProvince && province.push(provinces.find(item => item.code === provinceCode))
                    fragment = fragment.replace(replaceName, '')
                    break
                }
            }
        } else {
            // 没有省市，区县市有可能重名，这里暂时不处理，因为概率极低，可以根据添加市解决
            for (let i = name.length; i > 1; i--) {
                const replaceName = name.substring(0, i)
                if (fragment.indexOf(replaceName) === 0) {
                    area.push(tempArea)
                    city.push(cities.find(item => item.code === cityCode))
                    province.push(provinces.find(item => item.code === provinceCode))
                    fragment = fragment.replace(replaceName, '')
                    break
                }
            }
            if (area.length > 0) {
                break
            }
        }
    }

    // 从街道开始查找
    for (const tempStreet of streets) {
        console.log('tempStreet',tempStreet)
        const {name, provinceCode, cityCode, areaCode} = tempStreet
        const currentProvince = province[0]
        const currentCity = city[0]
        const currentArea = area[0]

        // 有省或者市或地区
        if (currentProvince || currentCity || currentArea) {
            if ((currentProvince && currentProvince.code === provinceCode)
                || (currentCity && currentCity.code) === cityCode
                || (currentArea && currentArea.code) === areaCode) {
                let replaceName = ''
                for (let i = name.length; i > 1; i--) {
                    const temp = name.substring(0, i)
                    if (fragment.indexOf(temp) === 0) {
                        replaceName = temp
                        break
                    }
                }
                if (replaceName) {
                    street.push(tempStreet)
                    !currentArea && area.push(areas.find(item => item.code === areaCode))
                    !currentCity && city.push(cities.find(item => item.code === cityCode))
                    !currentProvince && province.push(provinces.find(item => item.code === provinceCode))
                    fragment = fragment.replace(replaceName, '')
                    break
                }
            }
        } else {
            // 没有省市，区县市有可能重名，这里暂时不处理，因为概率极低，可以根据添加市解决
            for (let i = name.length; i > 1; i--) {
                const replaceName = name.substring(0, i)
                if (fragment.indexOf(replaceName) === 0) {
                    street.push(tempStreet)
                    area.push(streets.find(item => item.code === areaCode))
                    city.push(cities.find(item => item.code === cityCode))
                    province.push(provinces.find(item => item.code === provinceCode))
                    fragment = fragment.replace(replaceName, '')
                    break
                }
            }
            if (street.length > 0) {
                break
            }
        }
    }

    // 解析完省市区如果还存在地址，则默认为详细地址
    if (fragment.length > 0) {
        detail.push(fragment)
    }

    return {
        province,
        city,
        area,
        detail,
    }
}

/**
 * 判断是否是名字
 * @param fragment
 * @returns {string}
 */
const judgeFragmentIsName = (fragment, nameMaxLength) => {
    if (!fragment || !/[\u4E00-\u9FA5]/.test(fragment)) {
        return ''
    }

    // 如果包含下列称呼，则认为是名字，可自行添加
    const nameCall = ['先生', '小姐', '同志', '哥哥', '姐姐', '妹妹', '弟弟', '妈妈', '爸爸', '爷爷', '奶奶', '姑姑', '舅舅']
    if (nameCall.find(item => fragment.indexOf(item) !== -1)) {
        return fragment
    }

    // 如果百家姓里面能找到这个姓，并且长度在1-5之间
    const nameFirst = fragment.substring(0, 1)
    if (fragment.length <= nameMaxLength && fragment.length > 1 && zhCnNames.indexOf(nameFirst) !== -1) {
        return fragment
    }

    return ''
}

/**
 * 匹配电话
 * @param address
 * @returns {{address: *, phone: string}}
 */
const filterPhone = (address) => {
    let phone = ''
    // 整理电话格式
    address = address.replace(/(\d{3})-(\d{4})-(\d{4})/g, '$1$2$3')
    address = address.replace(/(\d{3}) (\d{4}) (\d{4})/g, '$1$2$3')
    address = address.replace(/(\d{4}) \d{4} \d{4}/g, '$1$2$3')
    address = address.replace(/(\d{4})/g, '$1')

    const mobileReg = /(\d{7,12})|(\d{3,4}-\d{6,8})|(86-[1][0-9]{10})|(86[1][0-9]{10})|([1][0-9]{10})/g
    const mobile = mobileReg.exec(address)
    if (mobile) {
        phone = mobile[0]
        address = address.replace(mobile[0], ' ')
    }
    return {address, phone}
}

/**
 * 匹配邮编
 * @param address
 * @returns {{address: *, postalCode: string}}
 */
const filterPostalCode = (address) => {
    let postalCode = ''
    const postalCodeReg = /\d{6}/g
    const code = postalCodeReg.exec(address)
    if (code) {
        postalCode = code[0]
        address = address.replace(code[0], ' ')
    }
    return {address, postalCode}
}

/**
 * 地址清洗
 * @param address
 * @returns {*}
 */
const cleanAddress = (address, textFilter = []) => {
    // 去换行等
    address = address
        .replace(/\r\n/g, ' ')
        .replace(/\n/g, ' ')
        .replace(/\t/g, ' ')

    // 自定义去除关键字，可自行添加
    const search = [
        '详细地址',
        '收货地址',
        '收件地址',
        '地址',
        '所在地区',
        '地区',
        '姓名',
        '收货人',
        '收件人',
        '联系人',
        '收',
        '邮编',
        '联系电话',
        '电话',
        '联系人手机号码',
        '手机号码',
        '手机号',
    ].concat(textFilter)
    search.forEach(str => {
        address = address.replace(new RegExp(str, 'g'), ' ')
    })

    const pattern = new RegExp("[`~!@#$^&*()=|{}':;',\\[\\]\.<>/?~！@#￥……&*（）——|{}【】‘；：”“’。，、？]", 'g')
    address = address.replace(pattern, ' ')

    // 多个空格replace为一个
    address = address.replace(/ {2,}/g, ' ')

    return address
}

export default AddressParse
