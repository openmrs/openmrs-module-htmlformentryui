/**
 * Returns date translated according to preferred locale when Date Format displays the month as an abbreviation (MMM).
 * Ex: Date: 02-Jan-2021 Format: DD-MMM-YYYY Locale: fr_FR --> Return: 02-janv.-2021
* @param {object} date Input Date
* @param {string} format Date Format
* @param {string} locale The preferred locale
*/

function formatDatetime(date, format, locale) {
    var defaultFormat = 'DD.MMM.YYYY, HH:mm:ss';
    var defaultLocale = 'en';

    try{
        if (format !== null && format !== undefined && format !== '') {
            if (format.match(/MMM/g)) {
                if (locale !== null && locale !== undefined && locale !== '') {
                    return format.replace("DD", moment(date).format('DD')).replace("MMM", new Date(date).toLocaleDateString(locale, {month: 'short'})).replace('YYYY', moment(date).format('YYYY'))
                        .replace('HH', moment(date).format('HH')).replace('mm', moment(date).format('mm')).replace('ss', moment(date).format('ss'));
                }
                return format.replace("DD", moment(date).format('DD')).replace("MMM", new Date(date).toLocaleDateString(defaultLocale, {month: 'short'})).replace('YYYY', moment(date).format('YYYY'))
                    .replace('HH', moment(date).format('HH')).replace('mm', moment(date).format('mm')).replace('ss', moment(date).format('ss'));
            }
            return moment(date).format(format.toUpperCase);
        }
        return moment(date).format(defaultFormat);
    } catch(err) {
        return moment(date).format(defaultFormat);
    }
}

function formatDate(date, format, locale) {
    var defaultFormat = 'DD.MMM.YYYY';
    var defaultLocale = 'en';

    try{
        if (format !== null && format !== undefined && format !== '') {
            if (format.match(/MMM/g)) {
                if (locale !== null && locale !== undefined && locale !== '') {
                    return format.toUpperCase().replace("DD", moment(date).format('DD')).replace("MMM", new Date(date).toLocaleDateString(locale, {month: 'short'})).replace('YYYY', moment(date).format('YYYY'));
                }
                return format.toUpperCase().replace("DD", moment(date).format('DD')).replace("MMM", new Date(date).toLocaleDateString(defaultLocale, {month: 'short'})).replace('YYYY', moment(date).format('YYYY'));
            }
            return moment(date).format(format.toUpperCase);
        }
        return moment(date).format(defaultFormat);
    } catch(err) {
        return moment(date).format(defaultFormat);
    }
}

function formatTime(date, format) {
    var defaultFormat = 'HH:mm';
    try{
        return moment(date).format(format);
    } catch(err) {
        return moment(date).format(defaultFormat);
    }
}

/*
function translateDate(date, format, locale){
    var defaultFormat = 'DD.MMM.YYYY';
    try {
        moment.locale(locale);
        return moment(date).format(format);
    } catch(err) {
        alert(err)
        return moment(date).format(defaultFormat);
    }
}


function translateDateTime(date, format, locale){
    alert(date)
    alert(format)
    alert(locale)
    var defaultFormat = 'DD.MMM.YYYY, HH:mm:ss';
    try {
            moment.locale('es');
            return moment(date).format(format);
    } catch(err) {
        alert(err)
        return moment(date).format(defaultFormat);
    }*/

