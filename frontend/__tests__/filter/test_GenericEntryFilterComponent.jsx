import React from 'react';
import {shallow,mount} from 'enzyme';
import GenericEntryFilterComponent from '../../app/filter/GenericEntryFilterComponent.jsx';
import sinon from 'sinon';

class MyTestFilterComponent extends GenericEntryFilterComponent {
    constructor(props){
        super(props);

        this.state = Object.assign(this.state, {
            selectorValues: ["value one","value two"]
        });

        this.filterSpec = [
            {
                key: "field1",
                label: "Field 1"
            },
            {
                key: "numericField",
                label: "Numeric Field",
                validator: value=>{try { return parseInt(value); } catch(e) { return null }}
            },
            {
                key: "selectorField",
                label: "Selector Field",
                valuesStateKey: "selectorValues"
            }
        ];

    }
}

describe("GenericEntryFilterComponent", ()=>{
    it("should render a form with the right controls in it", ()=>{
        const updateCb = sinon.spy();

        const rendered = shallow(<MyTestFilterComponent filterDidUpdate={updateCb} filterTerms={{}}/>);

        expect(rendered.find('input#field1').exists()).toBeTruthy();
        expect(rendered.find('input#numericField').exists()).toBeTruthy();
        expect(rendered.find('input#dfsfsjkf').exists()).toBeFalsy();

        const selector = rendered.find('select#selectorField');
        expect(selector.exists()).toBeTruthy();
        expect(selector.find('option[name="value one"]').exists()).toBeTruthy();
        expect(selector.find('option[name="value two"]').exists()).toBeTruthy();
    });

    it("should call the update callback if any value changes", ()=>{
        const updateCb = sinon.spy();

        const rendered = shallow(<MyTestFilterComponent filterDidUpdate={updateCb} filterTerms={{}}/>);

        rendered.find('input#field1').simulate('change',{target: {value: "newsearch"}});
        expect(updateCb.callCount).toEqual(1);
        expect(updateCb.calledWith({field1: "newsearch", match: "W_CONTAINS"})).toBeTruthy();

        rendered.find('select#selectorField').simulate('change',{target: {value: "value two"}});
        expect(updateCb.callCount).toEqual(2);
        expect(updateCb.calledWith({field1: "newsearch", selectorField:"value two", match: "W_CONTAINS"})).toBeTruthy();
    })
});