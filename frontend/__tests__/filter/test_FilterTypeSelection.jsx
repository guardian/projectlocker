import React from 'react';
import {shallow,mount} from 'enzyme';
import FilterTypeSelection from '../../app/filter/FilterTypeSelection.jsx';
import sinon from 'sinon';

describe("FilterTypeSelection", ()=>{
    it("should present radio buttons for each option" ,()=>{
        const onChangedCb = sinon.spy();
        const rendered = shallow(<FilterTypeSelection type="W_CONTAINS" selectionChanged={onChangedCb}/>);

        expect(rendered.find('input').length).toEqual(4);
        const containsElem = rendered.find('input[value="W_CONTAINS"]');
        expect(containsElem.length).toEqual(1);
        expect(containsElem.prop("defaultChecked")).toEqual(true);

        const startsElem = rendered.find('input[value="W_STARTSWITH"]');
        expect(startsElem.length).toEqual(1);
        expect(startsElem.prop("defaultChecked")).toEqual(false);

        const endsElem = rendered.find('input[value="W_ENDSWITH"]');
        expect(endsElem.length).toEqual(1);
        expect(endsElem.prop("defaultChecked")).toEqual(false);

        const exactElem = rendered.find('input[value="W_EXACT"]');
        expect(exactElem.length).toEqual(1);
        expect(exactElem.prop("defaultChecked")).toEqual(false);
    });

    it("should call the callback if the value changes", ()=>{
        const onChangedCb = sinon.spy();
        const rendered = shallow(<FilterTypeSelection type="W_CONTAINS" selectionChanged={onChangedCb}/>);

        const listContainer = rendered.find('ul');
        listContainer.simulate("change", {target: {value: "W_ENDSWITH"}});

        expect(onChangedCb.calledWith("W_ENDSWITH")).toBeTruthy();
    });

    it("should reflect the type prop in the radio button selection state", ()=>{
        const onChangedCb = sinon.spy();
        const rendered = shallow(<FilterTypeSelection type="W_STARTSWITH" selectionChanged={onChangedCb}/>);

        expect(rendered.find('input').length).toEqual(4);
        const containsElem = rendered.find('input[value="W_CONTAINS"]');
        expect(containsElem.length).toEqual(1);
        expect(containsElem.prop("defaultChecked")).toEqual(false);

        const startsElem = rendered.find('input[value="W_STARTSWITH"]');
        expect(startsElem.length).toEqual(1);
        expect(startsElem.prop("defaultChecked")).toEqual(true);
    })
});