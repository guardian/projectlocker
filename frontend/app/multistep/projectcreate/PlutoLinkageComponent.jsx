import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import WorkingGroupSelector from '../common/WorkingGroupSelector.jsx';
import CommissionSelector from '../common/CommissionSelector.jsx';

class PlutoLinkageComponent extends CommonMultistepComponent {
    static propTypes = {
        valueWasSet: PropTypes.func.isRequired,
        currentPlutoCommission: PropTypes.number,
        currentWorkingGroup: PropTypes.number,
        workingGroupList: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            plutoCommissionRef: null,
            workingGroupRef: null
        }
    }

    componentWillMount(){
        this.setState({plutoCommissionRef: this.props.currentPlutoCommission, workingGroupRef: this.props.currentWorkingGroup})
    }

    render(){
        return <div>
            <h3>Select Commission</h3>
            <p>We need to know what piece of work this project file relates to.  Please select the relevant working group and commission</p>
            <table>
                <tbody>
                <tr>
                    <td>Working group</td>
                    <td><WorkingGroupSelector valueWasSet={value=>this.setState({workingGroupRef: value})}
                                          workingGroupList={this.props.workingGroupList}
                                          currentValue={this.state.workingGroupRef}/></td>
                </tr>
                <tr>
                    <td>Commission</td>
                    <td><CommissionSelector workingGroupId={this.state.workingGroupRef}
                                            selectedCommissionId={this.state.plutoCommissionRef}
                                            valueWasSet={value=>this.setState({plutoCommissionRef: value})}/>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default PlutoLinkageComponent;