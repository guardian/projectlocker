import React from 'react';
import {shallow,mount} from 'enzyme';
import sinon from 'sinon';
import CommissionSelector from "../../../app/multistep/common/CommissionSelector";

describe("CommissionSelector", ()=>{
    it("should prepare content correctly", ()=>{
        const result = CommissionSelector.convertContent({result: [{
            id: 1234,
                title: "Some commission",
                created: "2019-05-06T03:04:05"
            }]});

        expect(result).toEqual([{
            name: "Some commission", value: 1234
        }])
    });

    it("should make a searchdoc", ()=>{
        const valueChangedSpy = sinon.spy();
        const rendered = shallow(<CommissionSelector workingGroupId={1} selectedCommissionId={2} showStatus="In production" valueWasSet={valueChangedSpy}/>);

        const result = rendered.instance().makeSearchDoc("sometext");
        expect(result).toEqual({
            title: "sometext",
            workingGroupId: 1,
            status: "In production",
            match: "W_CONTAINS"
        });
    });

    it("should increment the refresh counter if the working group id changes", ()=>{
        const valueChangedSpy = sinon.spy();
        const rendered = shallow(<CommissionSelector workingGroupId={1} selectedCommissionId={2} showStatus="In production" valueWasSet={valueChangedSpy}/>);

        expect(rendered.find("FilterableList").props().triggerRefresh).toEqual(0);
        rendered.setProps({workingGroupId: 3});
        rendered.update();
        expect(rendered.find("FilterableList").props().triggerRefresh).toEqual(1);
    });

    it("should call its own callback when FilterableList signals an update", ()=>{
        const valueChangedSpy = sinon.spy();
        const rendered = shallow(<CommissionSelector workingGroupId={1} selectedCommissionId={2} showStatus="In production" valueWasSet={valueChangedSpy}/>);

        const list = rendered.find("FilterableList");
        list.props().onChange("10");

        const calls = valueChangedSpy.getCalls();
        expect(calls.length).toEqual(1);
        expect(valueChangedSpy.args[0]).toEqual([10])
    })
});