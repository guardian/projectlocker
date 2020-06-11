import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import WorkingGroupSelector from '../common/WorkingGroupSelector.jsx';
import CommissionSelector from '../common/CommissionSelector.jsx';

class PlutoLinkageComponent extends CommonMultistepComponent {
    static propTypes = {
        valueWasSet: PropTypes.func.isRequired,
        currentWorkingGroup: PropTypes.number,
        workingGroupList: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            showStatus: "In production"
        }
    }

    render(){
        return <div>
            <h3>Select Commission</h3>
            <p>We need to know which working group is undertaking this project.  Please select the relevant working group and if you are unsure which to choose ask your commissioning editor.</p>
            <table>
                <tbody>
                <tr>
                    <td>Working group</td>
                    <td><WorkingGroupSelector valueWasSet={value=>this.props.valueWasSet({workingGroupRef: value})}
                                              workingGroupList={this.props.workingGroupList}
                                              currentValue={this.props.workingGroupRef}/></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default PlutoLinkageComponent;